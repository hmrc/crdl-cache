package uk.gov.hmrc.crdlcache.schedulers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mockito.ArgumentMatchers.{any, anyLong, eq as equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mongodb.scala.{ClientSession, MongoClient, MongoDatabase, SingleObservable}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.CustomsOffice.fromDpsCustomOfficeList
import uk.gov.hmrc.crdlcache.models.CustomsOfficeListsInstruction
import uk.gov.hmrc.crdlcache.models.CustomsOfficeListsInstruction.UpsertCustomsOffice
import uk.gov.hmrc.crdlcache.models.dps.Relation
import uk.gov.hmrc.crdlcache.models.dps.RelationType.{Next, Prev, Self}
import uk.gov.hmrc.crdlcache.models.dps.col.{
  CustomsOfficeListResponse,
  DpsCustomsOffice,
  DpsCustomsOfficeDetail,
  DpsCustomsOfficeTimetable,
  DpsRoleTrafficCompetence,
  DpsTimetableLine,
  RDEntryStatus,
  SpecificNotes
}
import uk.gov.hmrc.crdlcache.repositories.CustomsOfficeListsRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class ImportCustomsOfficesListJobSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterEach {
  private val mongoComponent              = mock[MongoComponent]
  private val mongoClient                 = mock[MongoClient]
  private val mongoDatabase               = mock[MongoDatabase]
  private val clientSession               = mock[ClientSession]
  private val lockRepository              = mock[MongoLockRepository]
  private val customsOfficeListRepository = mock[CustomsOfficeListsRepository]
  private val dpsConnector                = mock[DpsConnector]
  private val fixedInstant                = Instant.parse("2025-06-03T00:00:00Z")
  private val clock                       = Clock.fixed(fixedInstant, ZoneOffset.UTC)

  given ActorSystem      = ActorSystem("test")
  given ExecutionContext = ExecutionContext.global

  private val customsOfficeListsJob = new ImportCustomsOfficesListJob(
    mongoComponent,
    lockRepository,
    customsOfficeListRepository,
    dpsConnector,
    clock
  )

  private val customsOfficeListPage1 = CustomsOfficeListResponse(
    List(
      DpsCustomsOffice(
        RDEntryStatus("valid", "01-05-2025"),
        "IT223100",
        None,
        Some("ITP00002"),
        None,
        None,
        None,
        "IT",
        Some("test@test.it"),
        None,
        Some("20250501"),
        None,
        "40121",
        "0039 435345",
        Some("0039 435345"),
        None,
        Some("Q"),
        None,
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          DpsCustomsOfficeDetail(
            "EMILIA 1 BOLOGNA",
            "IT",
            "BOLOGNA",
            "0",
            Some("A"),
            None,
            "1",
            "VIALE PIETRAMELLARA, 1/2"
          )
        ),
        DpsCustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20180101",
          "20991231",
          List(
            DpsTimetableLine(
              "1",
              "0800",
              "1800",
              "6",
              None,
              None,
              List(
                DpsRoleTrafficCompetence("EXC", "R"),
                DpsRoleTrafficCompetence("REG", "N/A"),
                DpsRoleTrafficCompetence("SCO", "N/A"),
                DpsRoleTrafficCompetence("PLA", "N/A"),
                DpsRoleTrafficCompetence("DIS", "N/A"),
                DpsRoleTrafficCompetence("RFC", "N/A"),
                DpsRoleTrafficCompetence("EXT", "N/A"),
                DpsRoleTrafficCompetence("EXP", "N/A"),
                DpsRoleTrafficCompetence("IPR", "N/A")
              )
            )
          )
        )
      ),
      DpsCustomsOffice(
        RDEntryStatus("valid", "01-05-2025"),
        "IT223101",
        None,
        Some("ITP00002"),
        Some("IT223101"),
        Some("IT223101"),
        None,
        "IT",
        Some("test@it"),
        None,
        Some("20250501"),
        None,
        "40131",
        "1234 045483382",
        Some("2343 34543"),
        None,
        Some("Q"),
        None,
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          DpsCustomsOfficeDetail(
            "AEROPORTO DI BOLOGNA",
            "IT",
            "BOLOGNA",
            "0",
            Some("A"),
            None,
            "1",
            "VIA DELL'AEROPORTO, 1"
          )
        ),
        DpsCustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20180101",
          "20991231",
          List(
            DpsTimetableLine(
              "1",
              "0000",
              "2359",
              "6",
              None,
              None,
              List(
                DpsRoleTrafficCompetence("DEP", "AIR"),
                DpsRoleTrafficCompetence("INC", "AIR"),
                DpsRoleTrafficCompetence("TXT", "AIR"),
                DpsRoleTrafficCompetence("DES", "AIR"),
                DpsRoleTrafficCompetence("ENQ", "N/A"),
                DpsRoleTrafficCompetence("ENT", "AIR"),
                DpsRoleTrafficCompetence("EXC", "N/A"),
                DpsRoleTrafficCompetence("EXP", "AIR"),
                DpsRoleTrafficCompetence("EXT", "AIR"),
                DpsRoleTrafficCompetence("REC", "N/A"),
                DpsRoleTrafficCompetence("REG", "N/A"),
                DpsRoleTrafficCompetence("TRA", "AIR"),
                DpsRoleTrafficCompetence("EIN", "AIR"),
                DpsRoleTrafficCompetence("PLA", "N/A"),
                DpsRoleTrafficCompetence("DIS", "N/A"),
                DpsRoleTrafficCompetence("RFC", "N/A"),
                DpsRoleTrafficCompetence("IPR", "N/A")
              )
            )
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://vdp.nonprod.denodo.hip.ns2n.corp.hmrc.gov.uk:9443/server/central_reference_data_library/ws_iv_crdl_customs_office/views/iv_crdl_customs_office"
      ),
      Relation(
        Next,
        "?%24start_index=10&%24count=10"
      )
    )
  )

  private val customsOfficeListPage2 = CustomsOfficeListResponse(
    List(
      DpsCustomsOffice(
        RDEntryStatus("valid", "22-03-2025"),
        "DK003102",
        None,
        None,
        Some("DK003102"),
        Some("DK003102"),
        None,
        "DK",
        Some("test@dk"),
        None,
        None,
        None,
        "9850",
        "+45 342234 34543",
        None,
        None,
        None,
        None,
        "0",
        None,
        None,
        List(SpecificNotes("SN0009")),
        List(
          DpsCustomsOfficeDetail(
            "Hirtshals Toldekspedition",
            "DA",
            "Hirtshals",
            "0",
            None,
            None,
            "0",
            "Dalsagervej 7"
          )
        ),
        DpsCustomsOfficeTimetable(
          "1",
          None,
          "20180101",
          "20991231",
          List(
            DpsTimetableLine(
              "1",
              "0800",
              "1600",
              "5",
              None,
              None,
              List(
                DpsRoleTrafficCompetence("EXL", "P"),
                DpsRoleTrafficCompetence("EXL", "R"),
                DpsRoleTrafficCompetence("EXP", "P"),
                DpsRoleTrafficCompetence("EXP", "R"),
                DpsRoleTrafficCompetence("EXT", "P"),
                DpsRoleTrafficCompetence("EXT", "R"),
                DpsRoleTrafficCompetence("PLA", "R"),
                DpsRoleTrafficCompetence("RFC", "R"),
                DpsRoleTrafficCompetence("DIS", "N/A"),
                DpsRoleTrafficCompetence("IPR", "N/A"),
                DpsRoleTrafficCompetence("ENQ", "P"),
                DpsRoleTrafficCompetence("ENQ", "R"),
                DpsRoleTrafficCompetence("ENQ", "N/A"),
                DpsRoleTrafficCompetence("REC", "P"),
                DpsRoleTrafficCompetence("REC", "R"),
                DpsRoleTrafficCompetence("REC", "N/A")
              )
            )
          )
        )
      ),
      DpsCustomsOffice(
        RDEntryStatus("valid", "22-03-2025"),
        "IT314102",
        None,
        Some("ITP00023"),
        Some("IT314102"),
        Some("IT314102"),
        None,
        "IT",
        Some("testo@it"),
        None,
        None,
        None,
        "10043",
        "345 34234",
        None,
        None,
        None,
        None,
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          DpsCustomsOfficeDetail(
            "ORBASSANO",
            "IT",
            "ORBASSANO (TO)",
            "0",
            Some("A"),
            None,
            "1",
            "Prima Strada, 5"
          )
        ),
        DpsCustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20240101",
          "99991231",
          List(
            DpsTimetableLine(
              "1",
              "0800",
              "1800",
              "5",
              None,
              None,
              List(
                DpsRoleTrafficCompetence("DEP", "R"),
                DpsRoleTrafficCompetence("INC", "R"),
                DpsRoleTrafficCompetence("TRA", "R"),
                DpsRoleTrafficCompetence("EXP", "R"),
                DpsRoleTrafficCompetence("EIN", "R"),
                DpsRoleTrafficCompetence("ENT", "R"),
                DpsRoleTrafficCompetence("EXC", "R"),
                DpsRoleTrafficCompetence("DES", "R"),
                DpsRoleTrafficCompetence("GUA", "R"),
                DpsRoleTrafficCompetence("EXT", "R"),
                DpsRoleTrafficCompetence("REG", "R"),
                DpsRoleTrafficCompetence("REC", "R"),
                DpsRoleTrafficCompetence("IPR", "N/A"),
                DpsRoleTrafficCompetence("ENQ", "N/A")
              )
            )
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://vdp.nonprod.denodo.hip.ns2n.corp.hmrc.gov.uk:9443/server/central_reference_data_library/ws_iv_crdl_customs_office/views/iv_crdl_customs_office?%24start_index=40"
      ),
      Relation(
        Prev,
        "?%24start_index=30&%24count=10"
      ),
      Relation(
        Next,
        "?%24start_index=50&%24count=10"
      )
    )
  )

  override def beforeEach(): Unit = {
    reset(
      mongoComponent,
      mongoClient,
      mongoDatabase,
      clientSession,
      lockRepository,
      customsOfficeListRepository,
      dpsConnector
    )

    // Job lock
    val mockLock = mock[Lock]
    when(lockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(mockLock)))
    when(lockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

    // Transactions
    when(mongoComponent.client).thenReturn(mongoClient)
    when(mongoComponent.database).thenReturn(mongoDatabase)
    when(mongoClient.startSession(any())).thenReturn(SingleObservable(clientSession))
    when(clientSession.commitTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))
    when(clientSession.abortTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))

  }

  "ImportCustomsOfficeListJob.importCustomsOfficeLists" should "import the customs office lists" in {
    when(customsOfficeListRepository.fetchCustomsOfficeReferenceNumbers(equalTo(clientSession)))
      .thenReturn(Future.successful(Set.empty[String]))
    when(dpsConnector.fetchCustomsOfficeLists(using any()))
      .thenReturn(Source(List(customsOfficeListPage1, customsOfficeListPage2)))
    when(
      customsOfficeListRepository.executeInstructions(
        equalTo(clientSession),
        any[List[CustomsOfficeListsInstruction]]
      )
    )
      .thenReturn(Future.unit)
    customsOfficeListsJob.importCustomsOfficeLists().futureValue
    verify(customsOfficeListRepository, times(1)).executeInstructions(
      equalTo(clientSession),
      equalTo(
        List(
          UpsertCustomsOffice(fromDpsCustomOfficeList(customsOfficeListPage1.elements.head)),
          UpsertCustomsOffice(fromDpsCustomOfficeList(customsOfficeListPage1.elements.last)),
          UpsertCustomsOffice(fromDpsCustomOfficeList(customsOfficeListPage2.elements.head)),
          UpsertCustomsOffice(fromDpsCustomOfficeList(customsOfficeListPage2.elements.last))
        )
      )
    )
    verify(clientSession, times(1)).commitTransaction()//is it one time
  }

}
