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

import com.mongodb.client.model.{IndexModel, UpdateOptions}
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model.{Filters, Updates}
import uk.gov.hmrc.crdlcache.models.{CodeListCode, LastUpdated}
import uk.gov.hmrc.crdlcache.models.errors.MongoError
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.equal

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LastUpdatedRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[LastUpdated](
    mongoComponent,
    collectionName = "last-updated",
    domainFormat = LastUpdated.format,
    indexes = Seq.empty[IndexModel]
  ) {

  // This is a single-document collection
  override lazy val requiresTtlIndex: Boolean = false

  def fetchLastUpdated(code: CodeListCode): Future[Option[Instant]] = {
    collection.find(equal("codeListCode", code.code)).headOption().map(_.map(_.date))
  }

  def fetchAllLastUpdated(): Future[Seq[LastUpdated]] = {
    collection.find().toFuture()
  }

  def setLastUpdated(code: CodeListCode, instant: Instant): Future[Unit] = {
    collection
      .updateOne(
        equal("codeListCode", code.code),
        Updates.set("date", instant),
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
}
