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
import org.mongodb.scala.*
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import play.api.libs.json.*
import uk.gov.hmrc.crdlcache.models
import uk.gov.hmrc.crdlcache.models.errors.MongoError
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListEntry, CorrespondenceListInstruction}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CorrespondenceListsRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CodeListEntry](
    mongoComponent,
    collectionName = "correspondenceLists",
    domainFormat = CodeListEntry.mongoFormat,
    extraCodecs =
      Codecs.playFormatSumCodecs[JsValue](Format(Reads.JsValueReads, Writes.jsValueWrites)) ++
        Codecs.playFormatSumCodecs[JsBoolean](Format(Reads.JsBooleanReads, Writes.jsValueWrites)),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("codeListCode", "key", "value", "activeFrom"),
        IndexOptions().unique(true)
      ),
      IndexModel(
        Indexes.ascending("activeTo"),
        IndexOptions().expireAfter(30, TimeUnit.DAYS)
      )
    )
  )
  with Transactions {

  def fetchCorrespondenceKeys(
    session: ClientSession,
    code: CodeListCode
  ): Future[Set[(String, String)]] =
    collection
      .find(
        session,
        and(
          equal("codeListCode", code.code),
          equal("activeTo", null)
        )
      )
      .map(entry => (entry.key, entry.value))
      .toFuture()
      .map(_.toSet)

  def fetchCorrespondenceListEntries(
    code: CodeListCode,
    filterKeys: Option[Set[String]],
    activeAt: Instant
  ): Future[Seq[CodeListEntry]] = {
    val mandatoryFilters = List(
      equal("codeListCode", code.code),
      lte("activeFrom", activeAt),
      or(equal("activeTo", null), gt("activeTo", activeAt))
    )

    val keyFilters = filterKeys
      .map(ks => if ks.nonEmpty then List(in("key", ks.toSeq*)) else List.empty)
      .getOrElse(List.empty)

    val allFilters = mandatoryFilters ++ keyFilters

    collection
      .find(and(allFilters*))
      .sort(Sorts.ascending("key"))
      .toFuture()
  }

  private def deleteCorrespondenceListEntries(
    session: ClientSession,
    codeListCode: CodeListCode
  ): Future[Unit] =
    collection
      .deleteMany(
        session,
        equal("codeListCode", codeListCode.code)
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }

  def saveCorrespondenceListEntries(
    session: ClientSession,
    codeListCode: CodeListCode,
    entries: List[CodeListEntry]
  ): Future[Unit] =
    for {
      _ <- deleteCorrespondenceListEntries(session, codeListCode)

      _ <- collection.insertMany(session, entries).toFuture().map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }
    } yield ()

  private def supersedePreviousEntries(
    session: ClientSession,
    codeListCode: CodeListCode,
    key: String,
    value: String,
    activeFrom: Instant,
    includeActiveFrom: Boolean
  ): Future[Unit] = {
    collection
      .updateMany(
        session,
        and(
          equal("codeListCode", codeListCode.code),
          if includeActiveFrom then lte("activeFrom", activeFrom) else lt("activeFrom", activeFrom),
          equal("key", key),
          equal("value", value),
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

  private def upsertEntry(session: ClientSession, codeListEntry: CodeListEntry) =
    collection
      .replaceOne(
        session,
        and(
          equal("codeListCode", codeListEntry.codeListCode.code),
          equal("activeFrom", codeListEntry.activeFrom),
          equal("key", codeListEntry.key),
          equal("value", codeListEntry.value)
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

  def executeInstructions(
    session: ClientSession,
    instructions: List[CorrespondenceListInstruction]
  ): Future[Unit] =
    instructions.sortBy(_.activeFrom).foldLeft(Future.unit) {
      (previousInstruction, nextInstruction) =>
        previousInstruction.flatMap { _ =>
          nextInstruction match {
            case CorrespondenceListInstruction.UpsertEntry(codeListEntry) =>
              for {
                _ <- supersedePreviousEntries(
                  session,
                  codeListEntry.codeListCode,
                  codeListEntry.key,
                  codeListEntry.value,
                  codeListEntry.activeFrom,
                  includeActiveFrom = false
                )
                _ <- upsertEntry(session, codeListEntry)
              } yield ()
            case CorrespondenceListInstruction.InvalidateEntry(codeListEntry) =>
              supersedePreviousEntries(
                session,
                codeListEntry.codeListCode,
                codeListEntry.key,
                codeListEntry.value,
                codeListEntry.activeFrom,
                includeActiveFrom = true
              )
            case CorrespondenceListInstruction.RecordMissingEntry(
                  codeListCode,
                  key,
                  value,
                  removedAt
                ) =>
              supersedePreviousEntries(
                session,
                codeListCode,
                key,
                value,
                removedAt,
                includeActiveFrom = true
              )
          }
        }
    }
}
