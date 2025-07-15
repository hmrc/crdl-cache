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

package uk.gov.hmrc.crdlcache.controllers

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.crdlcache.repositories.CustomsOfficeListsRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import org.mockito.ArgumentMatchers.eq as equalTo
import uk.gov.hmrc.crdlcache.models.{
  CustomsOffice,
  CustomsOfficeDetail,
  CustomsOfficeTimetable,
  RoleTrafficCompetence,
  TimetableLine
}
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.libs.json.Json

import java.time.format.DateTimeFormatter
import java.time.{Clock, DayOfWeek, Instant, LocalDate, LocalTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class CustomsOfficeListsControllerSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with HttpClientV2Support
  with GuiceOneServerPerSuite
  with BeforeAndAfterEach {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  val repository = mock[CustomsOfficeListsRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  override def beforeEach(): Unit = {
    reset(repository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[CustomsOfficeListsRepository].toInstance(repository),
        bind[HttpClientV2].toInstance(httpClientV2),
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
      )
      .build()
  private val dateFormat                      = DateTimeFormatter.ofPattern("yyyyMMdd")
  protected val timeFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

  private val office = List(
    CustomsOffice(
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
      Some("+45 342234 34543"),
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
            LocalTime.parse("08:00", timeFormat),
            LocalTime.parse("16:00", timeFormat),
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
  )

  val responseJson = Json.obj(
    "referenceNumber"                             -> "DK003102",
    "referenceNumberMainOffice"                   -> null,
    "referenceNumberHigherAuthority"              -> null,
    "referenceNumberCompetentAuthorityOfEnquiry"  -> "DK003102",
    "referenceNumberCompetentAuthorityOfRecovery" -> "DK003102",
    "referenceNumberTakeover"                     -> null,
    "countryCode"                                 -> "DK",
    "emailAddress"                                -> "test@dk",
    "unLocodeId"                                  -> null,
    "nctsEntryDate"                               -> null,
    "nearestOffice"                               -> null,
    "postalCode"                                  -> "9850",
    "phoneNumber"                                 -> "+45 342234 34543",
    "faxNumber"                                   -> null,
    "telexNumber"                                 -> null,
    "geoInfoCode"                                 -> null,
    "regionCode"                                  -> null,
    "traderDedicated"                             -> false,
    "dedicatedTraderLanguageCode"                 -> null,
    "dedicatedTraderName"                         -> null,
    "customsOfficeSpecificNotesCodes"             -> Json.arr("SN0009"),
    "customsOfficeLsd" -> Json.obj(
      "city"                   -> "Hirtshals",
      "languageCode"           -> "DA",
      "spaceToAdd"             -> false,
      "customsOfficeUsualName" -> "Hirtshals Toldekspedition",
      "prefixSuffixFlag"       -> false,
      "streetAndNumber"        -> "Dalsagervej 7"
    ),
    "customsOfficeTimetable" -> Json.obj(
      "seasonCode"      -> 1,
      "seasonStartDate" -> "2018-01-01",
      "seasonEndDate"   -> "2099-12-31",
      "customsOfficeTimetableLine" -> Json.arr(
        Json.obj(
          "dayInTheWeekEndDay"              -> 5,
          "openingHoursTimeFirstPeriodFrom" -> "08:00:00",
          "dayInTheWeekBeginDay"            -> 1,
          "openingHoursTimeFirstPeriodTo"   -> "16:00:00",
          "customsOfficeRoleTrafficCompetence" -> Json.arr(
            Json.obj(
              "roleName"    -> "EXL",
              "trafficType" -> "P"
            ),
            Json.obj(
              "roleName"    -> "EXL",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "EXP",
              "trafficType" -> "P"
            ),
            Json.obj(
              "roleName"    -> "EXP",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "EXT",
              "trafficType" -> "P"
            ),
            Json.obj(
              "roleName"    -> "EXT",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "PLA",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "RFC",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "DIS",
              "trafficType" -> "N/A"
            ),
            Json.obj(
              "roleName"    -> "IPR",
              "trafficType" -> "N/A"
            ),
            Json.obj(
              "roleName"    -> "ENQ",
              "trafficType" -> "P"
            ),
            Json.obj(
              "roleName"    -> "ENQ",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "ENQ",
              "trafficType" -> "N/A"
            ),
            Json.obj(
              "roleName"    -> "REC",
              "trafficType" -> "P"
            ),
            Json.obj(
              "roleName"    -> "REC",
              "trafficType" -> "R"
            ),
            Json.obj(
              "roleName"    -> "REC",
              "trafficType" -> "N/A"
            )
          )
        )
      )
    )
  )

  "CustomsOfficeListsController" should "return 200 OK when there are no errors" in {
    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(office))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/offices")
      .execute[HttpResponse]
      .futureValue

    response.json mustBe Json.arr(responseJson)
  }

  it should "return 200 OK when there are no offices to return" in {
    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(List.empty))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/offices")
      .execute[HttpResponse]
      .futureValue

    response.json mustBe Json.arr()
    response.status mustBe Status.OK
  }

  it should "return 400 Bad Request when the user provides an invalid activeAt timestamp" in {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?activeAt=2025-06-05")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "parse comma-separated countryCodes from a query parameter when there is only one country" in {
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("GB"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?countryCodes=GB")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated countryCodes from a query parameter when there are multiple countries" in {
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("GB", "XI"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?countryCodes=GB,XI")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated countryCodes when there are multiple declarations of the query parameter" in {
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("GB", "XI", "AW", "BL"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?countryCodes=GB,XI&countryCodes=AW,BL")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there is no value declared for the query parameter" in {
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set.empty)),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?countryCodes=")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {

    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

}
