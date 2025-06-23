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
import org.apache.pekko.stream.{ActorAttributes, DelayOverflowStrategy, Supervision}
import org.apache.pekko.stream.scaladsl.Sink
import org.mongodb.scala.ClientSession
import org.quartz.{Job, JobExecutionContext}
import play.api.Logging
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.CustomsOfficeListsInstruction.{
  RecordMissingCustomsOffice,
  UpsertCustomsOffice
}
import uk.gov.hmrc.crdlcache.models.{CustomsOffice, CustomsOfficeListsInstruction}
import uk.gov.hmrc.crdlcache.repositories.CustomsOfficeListsRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time.{Clock, LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class ImportCustomsOfficesListJob @Inject() (
  val mongoComponent: MongoComponent,
  val lockRepository: MongoLockRepository,
  customsOfficeListsRepository: CustomsOfficeListsRepository,
  dpsConnector: DpsConnector,
  clock: Clock
)(using system: ActorSystem, ec: ExecutionContext)
  extends Job
  with LockService
  with Logging
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict

  val lockId: String = "import-offices"
  val ttl: Duration  = 1.hour

  private[schedulers] def processCustomsOffices(
    session: ClientSession,
    customsOffices: List[CustomsOffice]
  ): Future[List[CustomsOfficeListsInstruction]] = {
    logger.info("Importing customs office lists")
    customsOfficeListsRepository.fetchCustomsOfficeReferenceNumbers(session).flatMap {
      existingReferenceNumbers =>
        val incomingReferenceNumbers = customsOffices
          .map(_.referenceNumber)
          .toSet
        logger.info(s"existingReferenceNumbers $existingReferenceNumbers")
        logger.info(s"incomingReferenceNumbers $incomingReferenceNumbers")

        val mergedReferenceNumbers = existingReferenceNumbers.union(incomingReferenceNumbers)
        logger.info(s"mergedReferenceNumbers $mergedReferenceNumbers")
        val incomingGroupedReferenceNumbers = customsOffices.toSet
        logger.info(s"incomingGroupedReferenceNumbers $incomingGroupedReferenceNumbers")
        val instructions = List.newBuilder[CustomsOfficeListsInstruction]

        for (referenceNumber <- mergedReferenceNumbers) {

          val hasExistingOffice = existingReferenceNumbers.contains(referenceNumber)
          val maybeNewOffice = incomingGroupedReferenceNumbers.find(
            _.referenceNumber == referenceNumber
          )

          logger.info(
            s"for referenceNumber $referenceNumber hasExistingOffice $hasExistingOffice maybeNewOffice $maybeNewOffice"
          )

          (hasExistingOffice, maybeNewOffice) match {
            case (_, Some(newOffice)) => instructions += UpsertCustomsOffice(newOffice)
            case (true, None) =>
              val startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC)
              instructions += RecordMissingCustomsOffice(referenceNumber, startOfToday.toInstant)
            case (false, None) =>
              throw IllegalStateException(
                "Impossible case - we have neither a new nor existing entry for a key"
              )
          }
        }
        logger.info(s"list of instructions $instructions")
        Future.successful(instructions.result())
    }
    // case 1
//              customsOfficeListsRepository.upsertOffice(//UpsertEntry
//                session,
//                newOffice
//              ) // there's no existing office with that reference number and new office details are available CREATE
    // there's no existing office with that reference number and new office details are NOT available
//            case (true, Some(newOffice)) => //case 2 & 1 can be combined with instructions
//              instructions += UpsertCustomsOffice(newOffice)
////              customsOfficeListsRepository.supersedeOffice(
////                session,
////                referenceNumber,
////                newOffice.activeFrom,
////                true
////              )
//              // there's an existing office with that reference number and new office details are available
//              // expire older customs office active from and set active to date and upsert new office EXPIRE and ADD
////              customsOfficeListsRepository.upsertOffice(
////                session,
////                newOffice
////              ) // add the new office details to the db

    // case 3
//              customsOfficeListsRepository.supersedeOffice(
//                session,
//                referenceNumber,
//                startOfToday.toInstant,
//                false//this was wrong to be false
//              ) // there's an existing office with that reference number and new office details are NOT available this means the old office value needs to be deleted/invalidated EXPIRE

    // logger.info("Importing customs office list completed successfully")
  }

  private[schedulers] def importCustomsOfficeLists(): Future[Unit] = {
    for {
      customOfficeLists <- dpsConnector.fetchCustomsOfficeLists
        .delay(1.second, DelayOverflowStrategy.backpressure)
        .mapConcat(_.elements)
        .map(CustomsOffice.fromDpsCustomOfficeList)
        .withAttributes(ActorAttributes.withSupervisionStrategy { err =>
          logger.error(
            s"Stopping customs office list import job due to exception",
            err
          )
          Supervision.stop
        })
        .runWith(Sink.seq)
      _ <- withSessionAndTransaction { session =>
        for {
          instructions <- processCustomsOffices(session, customOfficeLists.toList)
          _            <- customsOfficeListsRepository.executeInstructions(session, instructions)
        } yield ()
      }
    } yield ()
  }

  def execute(context: JobExecutionContext): Unit =
    Await.result(importCustomsOfficeLists(), Duration.Inf)
}
