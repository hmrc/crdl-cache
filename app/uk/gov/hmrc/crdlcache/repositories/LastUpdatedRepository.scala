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

import com.mongodb.client.model.{IndexModel, IndexOptions, UpdateOptions}
import org.mongodb.scala.*
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.{Filters, Indexes, Updates}
import uk.gov.hmrc.crdlcache.models.errors.MongoError
import uk.gov.hmrc.crdlcache.models.formats.MongoFormats
import uk.gov.hmrc.crdlcache.models.{CodeListCode, DomainAndPhase, LastUpdated}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.Transactions
import org.mongodb.scala.model.Sorts

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LastUpdatedRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[LastUpdated](
    mongoComponent,
    collectionName = "last-updated",
    domainFormat = MongoFormats.lastUpdatedFormat,
    // Note: logically, domain is more significant than phase, but I don't know if we can swap
    // them without recreating the existing Mongo collection, and currently it's not a problem
    indexes = Seq(
      IndexModel(Indexes.ascending("codeListCode", "phase", "domain"), IndexOptions().unique(true)),
      IndexModel(Indexes.ascending("codeListCode"), IndexOptions().unique(false)),
      IndexModel(Indexes.ascending("phase"), IndexOptions().unique(false)),
      IndexModel(Indexes.ascending("domain"), IndexOptions().unique(false))
    ),
    replaceIndexes = true
  )
  with Transactions {

  // This collection contains only one document per codelist
  override lazy val requiresTtlIndex: Boolean = false

  def fetchLastUpdated(
    codeListCode: CodeListCode,
    domainAndPhase: DomainAndPhase
  ): Future[Option[LastUpdated]] = {
    collection
      .find(
        and(
          Seq(equal("codeListCode", codeListCode.code))
            ++ domainAndPhase.maybeDomain.fold(None)(domain => Seq(equal("domain", domain)))
            ++ domainAndPhase.maybeDomainPhase.fold(None)(phase => Seq(equal("phase", phase)))*
        )
      )
      .headOption()
  }

  def fetchAllLastUpdated: Future[Seq[LastUpdated]] = {
    collection.find().toFuture()
  }

  def setLastUpdated(
    session: ClientSession,
    codeListCode: CodeListCode,
    snapshotVersion: Long,
    domainAndPhase: DomainAndPhase,
    lastUpdated: Instant
  ): Future[Unit] = {
    val filter = domainAndPhase match {
      case DomainAndPhase(Some(domain), Some(phase)) =>
        and(
          equal("codeListCode", codeListCode.code),
          equal("domain", domain),
          equal("phase", phase)
        )
      case DomainAndPhase(None, None) => equal("codeListCode", codeListCode.code)
      case _ =>
        throw Exception(
          "Both domain and phase must be provided together, or neither should be provided"
        )
    }

    collection
      .updateOne(
        session,
        filter,
        Updates.combine(
          Updates.set("lastUpdated", lastUpdated),
          Updates.set("snapshotVersion", snapshotVersion)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
        else if (result.getModifiedCount == 0 && result.getUpsertedId == BsonNull)
          throw MongoError.NoMatchingDocument
      }
  }

  private def snapshotFilters(
    codeListCode: Option[String],
    domainAndPhase: DomainAndPhase
  ): Seq[org.mongodb.scala.bson.conversions.Bson] = {
    val codeFilter   = codeListCode.map(c => regex("codeListCode", c, "i")).toSeq
    val domainFilter = domainAndPhase.maybeDomain.map(d => equal("domain", d)).toSeq
    val phaseFilter  = domainAndPhase.maybeDomainPhase.map(p => equal("phase", p)).toSeq
    codeFilter ++ domainFilter ++ phaseFilter
  }

  def fetchAllLastUpdatedPaged(
    pageNum: Int,
    pageSize: Int,
    codeListCode: Option[String] = None,
    domainAndPhase: DomainAndPhase
  ): Future[Seq[LastUpdated]] = {
    val filters = snapshotFilters(codeListCode, domainAndPhase)
    val query   = if (filters.isEmpty) collection.find() else collection.find(and(filters*))
    query
      .sort(Sorts.ascending("codeListCode"))
      .skip((pageNum - 1) * pageSize)
      .limit(pageSize)
      .toFuture()
  }

  def codeListCount(
    codeListCode: Option[String] = None,
    domainAndPhase: DomainAndPhase
  ): Future[Long] = {
    val filters = snapshotFilters(codeListCode, domainAndPhase)
    if (filters.isEmpty) collection.countDocuments().toFuture()
    else collection.countDocuments(and(filters*)).toFuture()
  }
}
