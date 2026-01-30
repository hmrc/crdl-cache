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
import org.quartz.{DisallowConcurrentExecution, Job, JobExecutionContext}
import play.api.Logging
import uk.gov.hmrc.crdlcache.config.{AppConfig, ListConfig, PhaseAndDomainListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListSnapshot, CodeListSnapshotEntry}
import uk.gov.hmrc.crdlcache.repositories.{CodeListsRepository, LastUpdatedRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time.{Clock, ZoneOffset}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

@DisallowConcurrentExecution
abstract class ImportCodeListsJob[K, I](
  val jobName: String,
  val mongoComponent: MongoComponent,
  val lockRepository: MongoLockRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  repository: CodeListsRepository[K, I],
  dpsConnector: DpsConnector,
  appConfig: AppConfig,
  clock: Clock
)(using system: ActorSystem, ec: ExecutionContext)
  extends Job
  with LockService
  with Logging
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict

  override val lockId: String = jobName
  override val ttl: Duration  = 1.hour

  protected def listConfigs: List[ListConfig]

  protected def keyOfEntry(codeListSnapshotEntry: CodeListSnapshotEntry): K

  protected def processEntry(codeListCode: CodeListCode, newEntry: CodeListSnapshotEntry): I

  protected def recordMissing(codeListCode: CodeListCode, key: K): I

  def processSnapshot(
    session: ClientSession,
    listConfig: ListConfig,
    newSnapshot: CodeListSnapshot
  ): Future[List[I]] = {
    logger.info(
      s"Importing ${listConfig.origin} codelist ${listConfig.code.code} (${newSnapshot.name}) version ${newSnapshot.version}"
    )

    repository.fetchEntryKeys(session, listConfig.code).map { currentKeySet =>
      val incomingKeySet = newSnapshot.entries.map(keyOfEntry)
      val mergedKeySet   = currentKeySet.union(incomingKeySet)

      val groupedEntries = newSnapshot.entries.toList.groupBy(keyOfEntry)

      val instructions = List.newBuilder[I]

      for (key <- mergedKeySet) {
        val hasExistingEntry = currentKeySet.contains(key)
        val maybeNewEntries  = groupedEntries.get(key)

        // In case of multiple entries for a given key with the same activation date,
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
              processEntry(listConfig.code, _)
            )

          case (true, None) =>
            instructions += recordMissing(listConfig.code, key)

          case _ =>
            throw IllegalStateException(
              "Impossible case - we have neither a new nor existing entry for a key"
            )
        }
      }

      instructions.result()
    }
  }

  protected def importCodeList(codeListConfig: ListConfig): Future[Unit] = {
    for {
      // Fetch last updated timestamp
      storedLastUpdated <- lastUpdatedRepository.fetchLastUpdated(codeListConfig.code)
      defaultLastUpdated = appConfig.defaultLastUpdated.atStartOfDay(ZoneOffset.UTC).toInstant
      lastUpdated        = storedLastUpdated.map(_.lastUpdated).getOrElse(defaultLastUpdated)

      _ = logger.info(
        s"Importing codelist ${codeListConfig.code.code} from DPS with last updated timestamp $lastUpdated"
      )

      (phase, domain) = codeListConfig match {
        case pd: PhaseAndDomainListConfig => (Some(pd.phase), Some(pd.domain))
        case _                            => (None, None)
      }

      _ <- dpsConnector
        .fetchCodeListSnapshots(codeListConfig.code, lastUpdated, phase, domain)
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
              _            <- repository.executeInstructions(session, instructions)
              _ <- lastUpdatedRepository.setLastUpdated(
                session,
                codeListConfig.code,
                snapshot.version,
                clock.instant()
              )
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
    val importCodeLists = Source(listConfigs)
      .mapAsyncUnordered(Runtime.getRuntime.availableProcessors())(importCodeList)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
      .run()
      .map(_ => ())

    importCodeLists.foreach(_ => logger.info(s"$jobName job completed successfully"))

    importCodeLists
  }

  def execute(context: JobExecutionContext): Unit =
    Await.result(
      withLock(importCodeLists()).map {
        _.getOrElse {
          logger.info(s"$jobName job lock could not be obtained")
        }
      },
      Duration.Inf
    )
}
