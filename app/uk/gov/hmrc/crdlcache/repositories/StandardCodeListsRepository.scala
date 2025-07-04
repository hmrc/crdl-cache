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

import org.mongodb.scala.*
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import play.api.libs.json.*
import uk.gov.hmrc.crdlcache.models
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.crdlcache.models.{CodeListEntry, Instruction}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StandardCodeListsRepository @Inject() (mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends CodeListsRepository[String, Instruction](
    mongoComponent,
    collectionName = "codelists",
    extraCodecs =
      Codecs.playFormatSumCodecs[JsValue](Format(Reads.JsValueReads, Writes.jsValueWrites)) ++
        Codecs.playFormatSumCodecs[JsBoolean](Format(Reads.JsBooleanReads, Writes.jsValueWrites)),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("codeListCode", "key", "activeFrom"),
        IndexOptions().unique(true)
      )
    )
  ) {

  override def activationDate(instruction: Instruction): Instant = instruction.activeFrom

  override def keyOfEntry(codeListEntry: CodeListEntry): String = codeListEntry.key

  override def filtersForKey(key: String): Seq[Bson] = Seq(equal("key", key))

  def executeInstruction(session: ClientSession, instruction: Instruction): Future[Unit] =
    instruction match {
      case UpsertEntry(codeListEntry) =>
        for {
          _ <- supersedePreviousEntries(
            session,
            codeListEntry.codeListCode,
            codeListEntry.key,
            codeListEntry.activeFrom,
            includeActiveFrom = false
          )
          _ <- upsertEntry(session, codeListEntry)
        } yield ()
      case InvalidateEntry(codeListEntry) =>
        supersedePreviousEntries(
          session,
          codeListEntry.codeListCode,
          codeListEntry.key,
          codeListEntry.activeFrom,
          includeActiveFrom = true
        )
      case RecordMissingEntry(codeListCode, key, removedAt) =>
        supersedePreviousEntries(
          session,
          codeListCode,
          key,
          removedAt,
          includeActiveFrom = true
        )
    }
}
