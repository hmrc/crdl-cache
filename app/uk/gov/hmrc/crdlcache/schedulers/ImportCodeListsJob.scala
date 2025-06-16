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
import org.apache.pekko.stream.{ActorAttributes, DelayOverflowStrategy, Supervision}
import org.mongodb.scala.ClientSession
import org.quartz.{Job, JobExecutionContext}
import play.api.Logging
import uk.gov.hmrc.crdlcache.config.{AppConfig, CodeListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.*
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.crdlcache.models.Operation.{Create, Delete, Invalidate, Update}
import uk.gov.hmrc.crdlcache.repositories.{CodeListsRepository, LastUpdatedRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time.{Clock, Instant, LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class ImportCodeListsJob @Inject() (
  val mongoComponent: MongoComponent,
  val lockRepository: MongoLockRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  codeListsRepository: CodeListsRepository,
  dpsConnector: DpsConnector,
  appConfig: AppConfig,
  clock: Clock
)(using system: ActorSystem, ec: ExecutionContext)
  extends Job
  with LockService
  with Logging
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict

  val lockId: String = "import-code-lists"
  val ttl: Duration  = 1.hour

  private def fetchCurrentEntries(
    session: ClientSession,
    codeListCode: CodeListCode
  ): Future[Set[String]] =
    codeListsRepository.fetchCodeListEntryKeys(session, codeListCode)

  private def executeInstructions(
    session: ClientSession,
    instructions: List[Instruction]
  ): Future[Unit] =
    codeListsRepository.executeInstructions(session, instructions)

  private def setLastUpdated(
    session: ClientSession,
    codeListCode: CodeListCode,
    snapshotVersion: Long,
    lastUpdated: Instant
  ) = lastUpdatedRepository.setLastUpdated(session, codeListCode, snapshotVersion, lastUpdated)

  private[schedulers] def processEntry(
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

  private[schedulers] def processSnapshot(
    session: ClientSession,
    codeListConfig: CodeListConfig,
    newSnapshot: CodeListSnapshot
  ): Future[List[Instruction]] = {
    logger.info(
      s"Importing ${codeListConfig.origin} codelist ${codeListConfig.code.code} (${newSnapshot.name}) version ${newSnapshot.version}"
    )

    fetchCurrentEntries(session, codeListConfig.code).map { currentKeySet =>
      val incomingKeySet = newSnapshot.entries.map(_.key)
      val mergedKeySet   = currentKeySet.union(incomingKeySet)

      val groupedEntries = newSnapshot.entries.toList.groupBy(_.key)

      val instructions = List.newBuilder[Instruction]

      for (key <- mergedKeySet) {
        val hasExistingEntry = currentKeySet.contains(key)
        val maybeNewEntries  = groupedEntries.get(key)

        // In case of multiple entries for a given key with the same activation date,
        // the one with the latest modification timestamp is used.
        val entriesByDate = maybeNewEntries.map { newEntries =>
          newEntries
            .groupBy(_.activeFrom) // Group by activation date
            .view
            .mapValues(_.maxBy(_.updatedAt)) // Pick the latest modification
            .values
            .toSet
        }

        (hasExistingEntry, entriesByDate) match {
          case (_, Some(newEntries)) =>
            instructions ++= newEntries.map(
              processEntry(codeListConfig.code, _)
            )

          case (true, None) =>
            // DPS provides no snapshot dates:
            // the best we can do with removed entries is to use the start of today as their deactivation date
            val startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC)
            instructions += RecordMissingEntry(codeListConfig.code, key, startOfToday.toInstant)

          case _ =>
            throw IllegalStateException(
              "Impossible case - we have neither a new nor existing entry for a key"
            )
        }
      }

      instructions.result()
    }
  }

  private def importCodeList(codeListConfig: CodeListConfig): Future[Unit] = {
    for {
      // Fetch last updated timestamp
      storedLastUpdated <- lastUpdatedRepository.fetchLastUpdated(codeListConfig.code)
      defaultLastUpdated = appConfig.defaultLastUpdated.atStartOfDay(ZoneOffset.UTC).toInstant
      lastUpdated        = storedLastUpdated.map(_.lastUpdated).getOrElse(defaultLastUpdated)

      _ = logger.info(
        s"Importing codelist ${codeListConfig.code.code} from DPS with last updated timestamp ${lastUpdated}"
      )

      _ <- dpsConnector
        .fetchCodeListSnapshots(codeListConfig.code, lastUpdated)
        // Add a delay between calls to avoid overwhelming DPS
        .delay(1.second, DelayOverflowStrategy.backpressure)
        .mapConcat(_.elements)
        .dropWhile { snapshot =>
          // Ignore snapshot versions that we already have
          storedLastUpdated.exists(_.snapshotVersion >= snapshot.snapshotversion)
        }
        .map(CodeListSnapshot.fromDpsSnapshot(codeListConfig, _))
        .mapAsync(1) { snapshot =>
          // Ensure that we roll back if something goes wrong processing the snapshot
          withSessionAndTransaction { session =>
            for {
              instructions <- processSnapshot(session, codeListConfig, snapshot)
              _            <- executeInstructions(session, instructions)
              _ <- setLastUpdated(session, codeListConfig.code, snapshot.version, clock.instant())
            } yield ()
          }
        }
        .withAttributes(ActorAttributes.withSupervisionStrategy { err =>
          logger.error(
            s"Stopping codelist ${codeListConfig.code.code} import job due to exception",
            err
          )
          Supervision.stop
        })
        .run()

    } yield ()
  }

  private[schedulers] def importCodeLists(): Future[Unit] = {
    val importCodeLists = Source(appConfig.codeListConfigs)
      .mapAsyncUnordered(Runtime.getRuntime.availableProcessors())(importCodeList)
      .run()
      .map(_ => ())

    importCodeLists.foreach(_ => logger.info("Importing codelists completed successfully"))

    importCodeLists
  }

  def execute(context: JobExecutionContext): Unit =
    Await.result(importCodeLists(), Duration.Inf)
}
