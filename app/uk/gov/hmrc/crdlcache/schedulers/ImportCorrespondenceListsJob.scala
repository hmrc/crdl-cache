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
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.config.{AppConfig, CorrespondenceListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.*
import uk.gov.hmrc.crdlcache.models.CodeListCode.E200
import uk.gov.hmrc.crdlcache.models.CorrespondenceListInstruction.{
  InvalidateEntry,
  RecordMissingEntry,
  UpsertEntry
}
import uk.gov.hmrc.crdlcache.models.Operation.{Create, Delete, Invalidate, Update}
import uk.gov.hmrc.crdlcache.repositories.{CorrespondenceListsRepository, LastUpdatedRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time.{Clock, Instant, LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal

class ImportCorrespondenceListsJob @Inject() (
  val mongoComponent: MongoComponent,
  val lockRepository: MongoLockRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  correspondenceListsRepository: CorrespondenceListsRepository,
  dpsConnector: DpsConnector,
  appConfig: AppConfig,
  clock: Clock
)(using system: ActorSystem, ec: ExecutionContext)
  extends Job
  with LockService
  with Logging
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict

  val lockId: String = "import-correspondence-lists"
  val ttl: Duration  = 1.hour

  // The date of the full SEED extract used to populate the E200 list
  val SeedExtractDate: Instant = Instant.parse("2024-12-27T10:53:17Z")

  private def fetchCurrentEntries(
    session: ClientSession,
    codeListCode: CodeListCode
  ): Future[Set[(String, String)]] =
    correspondenceListsRepository.fetchCorrespondenceKeys(session, codeListCode)

  private def executeInstructions(
    session: ClientSession,
    instructions: List[CorrespondenceListInstruction]
  ): Future[Unit] =
    correspondenceListsRepository.executeInstructions(session, instructions)

  private def setLastUpdated(
    session: ClientSession,
    codeListCode: CodeListCode,
    snapshotVersion: Long,
    lastUpdated: Instant
  ) = lastUpdatedRepository.setLastUpdated(session, codeListCode, snapshotVersion, lastUpdated)

  private[schedulers] def processEntry(
    codeListCode: CodeListCode,
    newEntry: CodeListSnapshotEntry
  ): CorrespondenceListInstruction = {
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
    correspondenceListConfig: CorrespondenceListConfig,
    newSnapshot: CodeListSnapshot
  ): Future[List[CorrespondenceListInstruction]] = {
    logger.info(
      s"Importing ${correspondenceListConfig.origin} codelist ${correspondenceListConfig.code.code} (${newSnapshot.name}) version ${newSnapshot.version}"
    )

    fetchCurrentEntries(session, correspondenceListConfig.code).map { currentKeySet =>
      val incomingKeySet = newSnapshot.entries.map(entry => (entry.key, entry.value))
      val mergedKeySet   = currentKeySet.union(incomingKeySet)

      val groupedEntries = newSnapshot.entries.toList.groupBy(entry => (entry.key, entry.value))

      val instructions = List.newBuilder[CorrespondenceListInstruction]

      for (mapping @ (key, value) <- mergedKeySet) {
        val hasExistingEntry = currentKeySet.contains(mapping)
        val maybeNewEntries  = groupedEntries.get(mapping)

        // In case of multiple entries for a given key->value mapping with the same activation date,
        // the one with the latest modification timestamp and latest action ID is used.
        val entriesByDate = maybeNewEntries.map { newEntries =>
          newEntries
            .groupBy(_.activeFrom) // Group by activation date
            .view
            .mapValues(
              _.maxBy(entry =>
                (
                  entry.updatedAt,
                  entry.properties.value.get("actionIdentification").flatMap(_.asOpt[String])
                )
              )
            ) // Pick the latest modification and SEED "ActionIdentification"
            .values
            .toSet
        }

        (hasExistingEntry, entriesByDate) match {
          case (_, Some(newEntries)) =>
            instructions ++= newEntries.map(
              processEntry(correspondenceListConfig.code, _)
            )

          case (true, None) =>
            // DPS provides no snapshot dates:
            // the best we can do with removed entries is to use the start of today as their deactivation date
            val startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC)
            instructions += RecordMissingEntry(
              correspondenceListConfig.code,
              key,
              value,
              startOfToday.toInstant
            )

          case _ =>
            throw IllegalStateException(
              "Impossible case - we have neither a new nor existing entry for a key->value mapping"
            )
        }
      }

      instructions.result()
    }
  }

  private def importCorrespondenceList(
    correspondenceListConfig: CorrespondenceListConfig
  ): Future[Unit] = {
    for {
      // Fetch last updated timestamp
      storedLastUpdated <- lastUpdatedRepository.fetchLastUpdated(correspondenceListConfig.code)
      defaultLastUpdated = appConfig.defaultLastUpdated.atStartOfDay(ZoneOffset.UTC).toInstant
      lastUpdated        = storedLastUpdated.map(_.lastUpdated).getOrElse(defaultLastUpdated)

      _ = logger.info(
        s"Importing correspondence list ${correspondenceListConfig.code.code} from DPS with last updated timestamp ${lastUpdated}"
      )

      _ <- dpsConnector
        .fetchCodeListSnapshots(correspondenceListConfig.code, lastUpdated)
        // Add a delay between calls to avoid overwhelming DPS
        .delay(1.second, DelayOverflowStrategy.backpressure)
        .mapConcat(_.elements)
        .dropWhile { snapshot =>
          // Ignore snapshot versions that we already have
          storedLastUpdated.exists(_.snapshotVersion >= snapshot.snapshotversion)
        }
        .map(CodeListSnapshot.fromDpsSnapshot(correspondenceListConfig, _))
        .mapAsync(1) { snapshot =>
          // Ensure that we roll back if something goes wrong processing the snapshot
          withSessionAndTransaction { session =>
            for {
              instructions <- processSnapshot(session, correspondenceListConfig, snapshot)
              _            <- executeInstructions(session, instructions)
              _ <- setLastUpdated(
                session,
                correspondenceListConfig.code,
                snapshot.version,
                clock.instant()
              )
            } yield ()
          }
        }
        .withAttributes(ActorAttributes.withSupervisionStrategy { err =>
          logger.error(
            s"Stopping correspondence list ${correspondenceListConfig.code.code} import job due to exception",
            err
          )
          Supervision.stop
        })
        .run()

    } yield ()
  }

  private[schedulers] def importStaticData(): Future[Unit] = {
    try {
      logger.info(s"Importing static data for SEED correspondence list ${E200.code}")
      val json    = Json.parse(getClass.getResourceAsStream("/data/E200.json"))
      val entries = Json.fromJson[List[CodeListEntry]](json).get
      withSessionAndTransaction { session =>
        for {
          _ <- correspondenceListsRepository.saveCorrespondenceListEntries(session, E200, entries)
          _ <- lastUpdatedRepository.setLastUpdated(session, E200, 0, SeedExtractDate)
        } yield ()
      }
    } catch {
      case NonFatal(err) =>
        logger.error(s"Error parsing static data for SEED correspondence list ${E200.code}", err)
        Future.failed(err)
    }
  }

  private[schedulers] def importCorrespondenceLists(): Future[Unit] = {
    val importStaticLists = Source.future(importStaticData())

    val importCorrespondenceLists = Source(appConfig.correspondenceListConfigs)
      .mapAsyncUnordered(Runtime.getRuntime.availableProcessors())(importCorrespondenceList)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

    val importAll = importStaticLists.concat(importCorrespondenceLists).run().map(_ => ())

    importAll.foreach(_ => logger.info("Importing correspondence lists completed successfully"))

    importAll
  }

  def execute(context: JobExecutionContext): Unit =
    Await.result(importCorrespondenceLists(), Duration.Inf)
}
