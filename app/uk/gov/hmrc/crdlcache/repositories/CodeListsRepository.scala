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
import org.mongodb.scala.*
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import play.api.libs.json.*
import uk.gov.hmrc.crdlcache.models
import uk.gov.hmrc.crdlcache.models.errors.MongoError
import uk.gov.hmrc.crdlcache.models.formats.MongoFormats
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.Transactions

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

abstract class CodeListsRepository[K, I](
  val mongoComponent: MongoComponent,
  collectionName: String,
  indexes: Seq[IndexModel],
  extraCodecs: Seq[Codec[?]]
)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CodeListEntry](
    mongoComponent,
    collectionName,
    domainFormat = MongoFormats.codeListEntryFormat,
    extraCodecs = extraCodecs,
    indexes = IndexModel(
      Indexes.ascending("activeTo"),
      IndexOptions().expireAfter(30, TimeUnit.DAYS)
    ) +: indexes
  )
  with Transactions {

  def activationDate(instruction: I): Instant

  def keyOfEntry(codeListEntry: CodeListEntry): K

  def filtersForKey(key: K): Seq[Bson]

  def executeInstruction(session: ClientSession, instruction: I): Future[Unit]

  def executeInstructions(session: ClientSession, instructions: List[I]): Future[Unit] =
    instructions.sortBy(activationDate).foldLeft(Future.unit) {
      (previousInstruction, nextInstruction) =>
        previousInstruction.flatMap(_ => executeInstruction(session, nextInstruction))
    }

  def fetchEntryKeys(session: ClientSession, code: CodeListCode): Future[Set[K]] =
    collection
      .find(session, and(equal("codeListCode", code.code), equal("activeTo", null)))
      .map(keyOfEntry)
      .toFuture()
      .map(_.toSet)

  def fetchEntries(
    code: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, JsValue]],
    activeAt: Instant
  ): Future[Seq[CodeListEntry]] = {
    val mandatoryFilters = List(
      equal("codeListCode", code.code),
      lte("activeFrom", activeAt),
      or(equal("activeTo", null), gt("activeTo", activeAt))
    )

    val keyFilters = filterKeys
      .map { ks =>
        if ks.nonEmpty
        then List(in("key", ks.toSeq*))
        else List.empty
      }
      .getOrElse(List.empty)

    val propertyFilters = filterProperties
      .map { props =>
        if props.nonEmpty
        then props.map((k, v) => equal(s"properties.$k", v))
        else List.empty
      }
      .getOrElse(List.empty)

    val allFilters = mandatoryFilters ++ keyFilters ++ propertyFilters

    collection
      .find(and(allFilters*))
      .sort(Sorts.ascending("key"))
      .toFuture()
  }

  protected def supersedePreviousEntries(
    session: ClientSession,
    codeListCode: CodeListCode,
    key: K,
    activeFrom: Instant,
    includeActiveFrom: Boolean
  ): Future[Unit] = {
    collection
      .updateMany(
        session,
        and(
          Seq(
            equal("codeListCode", codeListCode.code),
            if includeActiveFrom
            then lte("activeFrom", activeFrom)
            else lt("activeFrom", activeFrom),
            or(equal("activeTo", null), exists("activeTo", false))
          ) ++ filtersForKey(key)*
        ),
        Updates.set("activeTo", activeFrom)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }
  }

  protected def upsertEntry(session: ClientSession, codeListEntry: CodeListEntry): Future[Unit] =
    collection
      .replaceOne(
        session,
        and(
          Seq(
            equal("codeListCode", codeListEntry.codeListCode.code),
            equal("activeFrom", codeListEntry.activeFrom)
          ) ++ filtersForKey(keyOfEntry(codeListEntry))*
        ),
        codeListEntry,
        new ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
        else if (result.getModifiedCount == 0 && result.getUpsertedId == BsonNull)
          throw MongoError.NoMatchingDocument
      }
}
