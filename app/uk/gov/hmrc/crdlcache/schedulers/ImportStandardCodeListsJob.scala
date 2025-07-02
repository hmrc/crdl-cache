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

package uk.gov.hmrc.crdlcache.schedulers

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.crdlcache.config.{AppConfig, ListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.*
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.crdlcache.models.Operation.{Create, Delete, Invalidate, Update}
import uk.gov.hmrc.crdlcache.repositories.{LastUpdatedRepository, StandardCodeListsRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import java.time.{Clock, LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ImportStandardCodeListsJob @Inject() (
  mongoComponent: MongoComponent,
  lockRepository: MongoLockRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  codeListsRepository: StandardCodeListsRepository,
  dpsConnector: DpsConnector,
  appConfig: AppConfig,
  clock: Clock
)(using system: ActorSystem, ec: ExecutionContext)
  extends ImportCodeListsJob[String, Instruction](
    "import-code-lists",
    mongoComponent,
    lockRepository,
    lastUpdatedRepository,
    codeListsRepository,
    dpsConnector,
    appConfig,
    clock
  ) {

  override protected def listConfigs: List[ListConfig] =
    appConfig.codeListConfigs

  override protected def keyOfEntry(snapshotEntry: CodeListSnapshotEntry): String =
    snapshotEntry.key

  override protected def recordMissing(codeListCode: CodeListCode, key: String): Instruction = {
    // DPS provides no snapshot dates:
    // the best we can do with removed entries is to use the start of today as their deactivation date
    val startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC)
    RecordMissingEntry(codeListCode, key, removedAt = startOfToday.toInstant)
  }

  override def processEntry(
    codeListCode: CodeListCode,
    newEntry: CodeListSnapshotEntry
  ): Instruction = {
    val updatedEntry: CodeListEntry = CodeListEntry(
      codeListCode,
      newEntry.key,
      newEntry.value,
      newEntry.activeFrom,
      None,
      newEntry.updatedAt,
      newEntry.properties
    )

    newEntry.operation match {
      case Some(Create)     => UpsertEntry(updatedEntry)
      case Some(Update)     => UpsertEntry(updatedEntry)
      case Some(Invalidate) => InvalidateEntry(updatedEntry)
      case Some(Delete)     => InvalidateEntry(updatedEntry)
      case None             => UpsertEntry(updatedEntry)
    }
  }
}
