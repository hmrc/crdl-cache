package uk.gov.hmrc.crdlcache.schedulers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.DelayOverflowStrategy
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mongodb.scala.ClientSession
import org.quartz.{Job, JobExecutionContext}
import play.api.Logging
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.{CustomsOffice, Instruction}
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
  ): Unit = {
    logger.info("Importing customs office lists")
    customsOfficeListsRepository.fetchCustomsOfficeReferenceNumbers(session).map {
      existingReferenceNumbers =>
        val incomingReferenceNumbers = customsOffices
          .map(_.referenceNumber)
          .toSet // can I do this toSet here would there be duplicate customs offices?

        val removedReferenceNumbers = existingReferenceNumbers.diff(incomingReferenceNumbers)

        val mergedReferenceNumbers = existingReferenceNumbers.union(incomingReferenceNumbers)

        val incomingGroupedReferenceNumbers = customsOffices.toSet // .groupBy(_.referenceNumber)

        val instructions = List.newBuilder[Instruction]

        for (referenceNumber <- mergedReferenceNumbers) {
          val hasExistingOffice = existingReferenceNumbers.contains(referenceNumber)
          val maybeNewOffice = incomingGroupedReferenceNumbers.find(
            _.referenceNumber == referenceNumber
          ) // .filter(_.referenceNumber==referenceNumber)//.get(referenceNumber)

          (hasExistingOffice, maybeNewOffice) match {
            case (false, Some(newOffice)) =>
              customsOfficeListsRepository.upsertOffice(
                session,
                newOffice
              ) // there's no existing office with that reference number and new office details are available CREATE
            case (false, None) =>
              throw IllegalStateException(
                "Impossible case - we have neither a new nor existing entry for a key"
              ) // there's no existing office with that reference number and new office details are NOT available
            case (true, Some(newOffice)) =>
              customsOfficeListsRepository.supersedeOffice(
                session,
                referenceNumber,
                newOffice.activeFrom,
                true
              )
              // there's an existing office with that reference number and new office details are available
              // expire older customs office active from and set active to date and upsert new office EXPIRE and ADD
              customsOfficeListsRepository.upsertOffice(
                session,
                newOffice
              ) // add the new office details to the db
            case (true, None) =>
              val startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC)
              customsOfficeListsRepository.supersedeOffice(
                session,
                referenceNumber,
                startOfToday.toInstant,
                false
              ) // there's an existing office with that reference number and new office details are NOT available this means the old office value needs to be deleted/invalidated EXPIRE

          }
          logger.info("Importing customs office list completed successfully")
        }
    }

  }

  def importCustomsOfficeLists(): Future[Unit] = {
    for {
      customOfficeLists <- dpsConnector.fetchCustomsOfficeLists
        .delay(1.second, DelayOverflowStrategy.backpressure)
        .mapConcat(_.elements)
        .map(CustomsOffice.fromDpsCustomOfficeList)
        .runWith(Sink.seq)
      _ <- withSessionAndTransaction { session =>
        processCustomsOffices(session, customOfficeLists.toList)
      }
//        .mapAsync(1){ customsOffice =>
//        withSessionAndTransaction{
//          session =>
//            for {
//              instructions <- processCustomsOffices(session, customsOffice)//want to send a list of customs office here
//            }yield ()
//        }}
    } yield ()
  }
//  private[schedulers] def importCodeLists(): Future[Unit] = {
//    val importCodeLists = Source(appConfig.codeListConfigs)
//      .mapAsyncUnordered(Runtime.getRuntime.availableProcessors())(importCodeList)
//      .run()
//      .map(_ => ())
//
//    importCodeLists.foreach(_ => logger.info("Importing codelists completed successfully"))
//
//    importCodeLists
//  }

  def execute(context: JobExecutionContext): Unit =
    Await.result(importCustomsOfficeLists(), Duration.Inf)
}
