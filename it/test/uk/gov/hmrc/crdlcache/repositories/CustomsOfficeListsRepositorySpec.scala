package uk.gov.hmrc.crdlcache.repositories

import org.mongodb.scala.*
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.crdlcache.models.{CustomsOffice, CustomsOfficeDetail, CustomsOfficeTimetable, RoleTrafficCompetence, TimetableLine}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, IndexedMongoQueriesSupport, PlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.{Assertion, OptionValues}

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, LocalDate, LocalTime}

class CustomsOfficeListsRepositorySpec  extends AnyFlatSpec
  with PlayMongoRepositorySupport[CustomsOffice]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  given TransactionConfiguration = TransactionConfiguration.strict

  given ec: ExecutionContext = ExecutionContext.global

  override protected val repository: CustomsOfficeListsRepository = new CustomsOfficeListsRepository(mongoComponent)

  override given patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  def withCustomsOfficeEntries(
                           offices: Seq[CustomsOffice]
                         )(test: ClientSession => Future[Assertion]): Unit = {
    repository.collection.insertMany(offices).toFuture.futureValue
    repository.withSessionAndTransaction(test).futureValue
  }

  protected val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
  private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

  val DK003102 = CustomsOffice(
    "DK003102",
    Instant.parse("2025-03-22T00:00:00Z"),
    None,
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
    false,
    None,
    None,
    List("SN0009"),
    CustomsOfficeDetail(
      "Hirtshals Toldekspedition",
      "DA",
      "Hirtshals",
      false,
      None,
      None,
      false,
      "Dalsagervej 7"
    ),
    CustomsOfficeTimetable(
      1,
      None,
      LocalDate.parse("20180101", dateFormat),
      LocalDate.parse("20991231", dateFormat),
      List(
        TimetableLine(
          DayOfWeek.of(1),
          LocalTime.parse("0800", timeFormat),
          LocalTime.parse("1600", timeFormat),
          DayOfWeek.of(5),
          None,
          None,
          List(
            RoleTrafficCompetence("EXL", "P"),
            RoleTrafficCompetence("EXL", "R"),
            RoleTrafficCompetence("EXP", "P"),
            RoleTrafficCompetence("EXP", "R"),
            RoleTrafficCompetence("EXT", "P"),
            RoleTrafficCompetence("EXT", "R"),
            RoleTrafficCompetence("PLA", "R"),
            RoleTrafficCompetence("RFC", "R"),
            RoleTrafficCompetence("DIS", "N/A"),
            RoleTrafficCompetence("IPR", "N/A"),
            RoleTrafficCompetence("ENQ", "P"),
            RoleTrafficCompetence("ENQ", "R"),
            RoleTrafficCompetence("ENQ", "N/A"),
            RoleTrafficCompetence("REC", "P"),
            RoleTrafficCompetence("REC", "R"),
            RoleTrafficCompetence("REC", "N/A")
          )
        )
      )
    )
  )
  val invalidatedoffice =   CustomsOffice(
    "IT314102",
    Instant.parse("2025-03-22T00:00:00Z"),
    Some(Instant.parse("2026-05-22T00:00:00Z")),
    Some("ITP00023"),
    Some("IT314102"),
    Some("IT314102"),
    None,
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
    false,
    Some("IT"),
    Some("TIN"),
    List.empty,
    CustomsOfficeDetail(
        "ORBASSANO",
        "IT",
        "ORBASSANO (TO)",
      false,
        Some("A"),
        None,
      true,
        "Prima Strada, 5"
      ),
    CustomsOfficeTimetable(
      1,
      Some("ALL YEAR"),
      LocalDate.parse("20180101", dateFormat),
      LocalDate.parse("99991231",dateFormat),
      List(
        TimetableLine(
          DayOfWeek.of(1),
          LocalTime.parse("0800", timeFormat),
          LocalTime.parse("1800", timeFormat),
          DayOfWeek.of(5),
          None,
          None,
          List(
            RoleTrafficCompetence("DEP", "R"),
            RoleTrafficCompetence("INC", "R"),
            RoleTrafficCompetence("TRA", "R"),
            RoleTrafficCompetence("EXP", "R"),
            RoleTrafficCompetence("EIN", "R"),
            RoleTrafficCompetence("ENT", "R"),
            RoleTrafficCompetence("EXC", "R"),
            RoleTrafficCompetence("DES", "R"),
            RoleTrafficCompetence("GUA", "R"),
            RoleTrafficCompetence("EXT", "R"),
            RoleTrafficCompetence("REG", "R"),
            RoleTrafficCompetence("REC", "R"),
            RoleTrafficCompetence("IPR", "N/A"),
            RoleTrafficCompetence("ENQ", "N/A")
          )
        )
      )
    )
  )
  
  private val customsOffices = Seq(DK003102, invalidatedoffice)

  "CustomsOfficeListsRepository.fetchCustomsOfficeReferenceNumbers" should "return offices present in the database" in withCustomsOfficeEntries(customsOffices) {
    session =>
      repository.fetchCustomsOfficeReferenceNumbers(session).map(_ must contain("DK003102"))
  }

  it should "not return entries that are invalidated" in withCustomsOfficeEntries(customsOffices){
    session =>
      repository.fetchCustomsOfficeReferenceNumbers(session).map(_ mustNot contain("IT314102"))
  }


}
