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
import org.apache.pekko.stream.scaladsl.Source
import org.quartz.{Job, JobExecutionContext}
import uk.gov.hmrc.crdlcache.config.{AppConfig, CodeListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.CodeListEntry
import uk.gov.hmrc.crdlcache.repositories.LastUpdatedRepository
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.time.{ZoneOffset, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class ImportCodeListsJob @Inject() (
  val lockRepository: MongoLockRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  dpsConnector: DpsConnector,
  appConfig: AppConfig
)(using system: ActorSystem, ec: ExecutionContext)
  extends Job
  with LockService {
  val lockId: String = "import-code-lists"
  val ttl: Duration  = 1.hour

  // * Detect removed entries
  // * Deduplicate new entries
  // * Process lifecycle events
  // * Sort entries according to their modification date and time
  // * Sort entries according to their activation date

  // * Key sets?
  //   currentCodelist.keySet.diff(codelistSnapshot.keySet)

  def importCodeList(lastUpdated: ZonedDateTime)(codeListConfig: CodeListConfig): Future[Unit] =
    for {
      // TODO: Fetch any existing codelist data to use as the initial value
      // TODO: Choose a representation for the running fold over codelist snapshots
      outputCodeList <- dpsConnector
        .fetchCodeListSnapshots(codeListConfig.code, lastUpdated)
        .runFold(Map.empty[String, CodeListEntry]) { (codeList, codeListResponse) =>
          codeList
        }
      // TODO: Insert the updated codelist data
    } yield ()

  def importCodeLists(): Future[Unit] =
    for {
      // Fetch last updated date
      storedLastUpdated <- lastUpdatedRepository.fetchLastUpdated()
      defaultLastUpdated = appConfig.defaultLastUpdated.atStartOfDay(ZoneOffset.UTC)
      lastUpdated = storedLastUpdated.map(_.atZone(ZoneOffset.UTC)).getOrElse(defaultLastUpdated)
      // Import all configured code lists
      _ <- Source(appConfig.codeListConfigs)
        .mapAsyncUnordered(Runtime.getRuntime.availableProcessors())(importCodeList(lastUpdated))
        .run()
    } yield ()

  def execute(context: JobExecutionContext): Unit =
    Await.result(importCodeLists(), Duration.Inf)
}
