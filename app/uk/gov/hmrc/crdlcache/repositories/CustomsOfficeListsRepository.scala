/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.crdlcache.repositories

import com.mongodb.client.model.ReplaceOptions
import org.bson.codecs.Codec
import org.bson.conversions.Bson
import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Projections.*
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.crdlcache.models.CustomsOfficeListsInstruction.{
  RecordMissingCustomsOffice,
  UpsertCustomsOffice
}
import uk.gov.hmrc.crdlcache.models.errors.MongoError
import uk.gov.hmrc.crdlcache.models.formats.MongoFormats
import uk.gov.hmrc.crdlcache.models.{
  CustomsOffice,
  CustomsOfficeListsInstruction,
  CustomsOfficeSummary
}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsOfficeListsRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CustomsOffice](
    mongoComponent,
    collectionName = "customsOfficeLists",
    domainFormat = MongoFormats.customsOfficeFormat,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("referenceNumber", "activeFrom", "phase", "domain"),
        IndexOptions().unique(true)
      ),
      IndexModel(
        Indexes.ascending("countryCode", "referenceNumber", "activeFrom", "phase", "domain")
      ),
      IndexModel(
        Indexes.ascending(
          "customsOfficeTimetable.customsOfficeTimetableLine.customsOfficeRoleTrafficCompetence.roleName",
          "referenceNumber",
          "activeFrom",
          "phase",
          "domain"
        )
      ),
      IndexModel(Indexes.ascending("activeTo"), IndexOptions().expireAfter(30, TimeUnit.DAYS))
    ),
    extraCodecs = Seq(Codecs.playFormatCodec(CustomsOfficeSummary.format))
  )
  with Transactions
  with Logging {

  def fetchCustomsOfficeReferenceNumbers(session: ClientSession): Future[Set[String]] =
    collection
      .find(session, equal("activeTo", null))
      .map(_.referenceNumber)
      .toFuture()
      .map(_.toSet)

  def upsertOffice(session: ClientSession, customsOffice: CustomsOffice) =
    collection
      .replaceOne(
        session,
        Filters.and(
          equal("referenceNumber", customsOffice.referenceNumber),
          equal("activeFrom", customsOffice.activeFrom)
        ),
        customsOffice,
        new ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
        else if (result.getModifiedCount == 0 && result.getUpsertedId == BsonNull)
          throw MongoError.NoMatchingDocument
      }

  def supersedeOffice(
    session: ClientSession,
    referenceNumber: String,
    activeFrom: Instant,
    includeActiveFrom: Boolean
  ): Future[Unit] = {
    collection
      .updateMany(
        session,
        and(
          equal("referenceNumber", referenceNumber),
          if includeActiveFrom then lte("activeFrom", activeFrom) else lt("activeFrom", activeFrom),
          or(equal("activeTo", null), exists("activeTo", false))
        ),
        Updates.set("activeTo", activeFrom)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }
  }

  def executeInstructions(
    session: ClientSession,
    instructions: List[CustomsOfficeListsInstruction]
  ): Future[Unit] =
    instructions.sortBy(_.activeFrom).foldLeft(Future.unit) {
      (previousInstruction, nextInstruction) =>
        previousInstruction.flatMap { _ =>
          nextInstruction match {
            case UpsertCustomsOffice(customsOffice) =>
              logger.debug(
                s"Upserting customs office with reference number: ${customsOffice.referenceNumber}"
              )
              for {
                _ <- supersedeOffice(
                  session,
                  customsOffice.referenceNumber,
                  customsOffice.activeFrom,
                  includeActiveFrom = false
                )
                _ <- upsertOffice(session, customsOffice)
              } yield ()
            case RecordMissingCustomsOffice(referenceNumber, removedAt) =>
              logger.debug(
                s"Recording removal of customs office reference number: $referenceNumber"
              )
              supersedeOffice(session, referenceNumber, removedAt, true)
          }
        }
    }

  def fetchCustomsOfficeLists(
    referenceNumbers: Option[Set[String]],
    countryCodes: Option[Set[String]],
    roles: Option[Set[String]],
    activeAt: Instant,
    phase: Option[String],
    domain: Option[String]
  ): Future[Seq[CustomsOffice]] = {
    val mandatoryFilters =
      List(lte("activeFrom", activeAt), or(equal("activeTo", null), gt("activeTo", activeAt)))

    val referenceNumberFilters = referenceNumbers
      .map(referenceNumbers =>
        if referenceNumbers.nonEmpty then List(in("referenceNumber", referenceNumbers.toSeq*))
        else List.empty
      )
      .getOrElse(List.empty)

    val countryCodeFilters = countryCodes
      .map(countryCodes =>
        if countryCodes.nonEmpty then List(in("countryCode", countryCodes.toSeq*)) else List.empty
      )
      .getOrElse(List.empty)

    val roleFilters = roles
      .map(roles =>
        if roles.nonEmpty then
          List(
            in(
              "customsOfficeTimetable.customsOfficeTimetableLine.customsOfficeRoleTrafficCompetence.roleName",
              roles.toSeq*
            )
          )
        else List.empty
      )
      .getOrElse(List.empty)

    val phaseDomainFilter = (phase, domain) match {
      case (Some(p), Some(d)) => List(equal("phase", p), equal("domain", d))
      case (None, None)       => List(exists("phase", false), exists("domain", false))
      case _                  => List.empty
    }

    val allFilters =
      mandatoryFilters ++ referenceNumberFilters ++ countryCodeFilters ++ roleFilters ++ phaseDomainFilter

    collection
      .find(and(allFilters*))
      .toFuture()
  }

  def fetchCustomsOfficeSummaries(
    activeAt: Instant,
    pageNum: Int,
    pageSize: Int
  ): Future[Seq[CustomsOfficeSummary]] = {
    val filters = List(
      lte("activeFrom", activeAt),
      or(equal("activeTo", null), gt("activeTo", activeAt))
    )

    collection
      .find[CustomsOfficeSummary](and(filters*))
      .projection(
        fields(
          include("referenceNumber", "countryCode", "customsOfficeLsd.customsOfficeUsualName"),
          excludeId()
        )
      )
      .skip((pageNum - 1) * pageSize)
      .limit(pageSize)
      .toFuture()
  }

  def customsOfficesCount(): Future[Long] = collection.countDocuments().toFuture()
}
