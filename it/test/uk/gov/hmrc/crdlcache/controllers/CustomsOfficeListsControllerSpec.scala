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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import org.mockito.ArgumentMatchers.eq as equalTo
import uk.gov.hmrc.crdlcache.models.{CustomsOffice, CustomsOfficeDetail, CustomsOfficeTimetable, RoleTrafficCompetence, TimetableLine}
import org.mockito.Mockito.{reset, when}
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCustomsOfficeLists
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}
import uk.gov.hmrc.internalauth.client.modules.InternalAuthModule
import uk.gov.hmrc.internalauth.client.test.StubBehaviour

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

  private val authStub = mock[StubBehaviour]
  private val repository = mock[CustomsOfficeListsRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  override def beforeEach(): Unit = {
    reset(authStub)
    reset(repository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[InternalAuthModule]
      .overrides(
        bind[StubBehaviour].toInstance(authStub),
        bind[BackendAuthComponents].toProvider[BackendAuthStubProvider],
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
      List(
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
    "customsOfficeTimetable" -> Json.arr(Json.obj(
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
  ))

  "CustomsOfficeListsController" should "return 200 OK when there are no errors" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(None), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(office))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/offices")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.json mustBe Json.arr(responseJson)
  }

  it should "return 200 OK when there are no offices to return" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(None), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(List.empty))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/offices")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.json mustBe Json.arr()
    response.status mustBe Status.OK
  }

  it should "return 400 Bad Request when the user provides an invalid activeAt timestamp" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?activeAt=2025-06-05")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "parse comma-separated reference numbers, countryCodes and roles from a query parameter when there is only one country and role" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("IT223100"))),
        equalTo(Some(Set("GB"))),
        equalTo(Some(Set("AUT"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?referenceNumbers=IT223100&countryCodes=GB&roles=AUT")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated reference numbers, countryCodes and roles from a query parameter when there are multiple countries and roles" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("IT223100", "IT223101"))),
        equalTo(Some(Set("GB", "XI"))),
        equalTo(Some(Set("AUT", "CCA"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?referenceNumbers=IT223100,IT223101&countryCodes=GB,XI&roles=AUT,CCA")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated reference numbers, countryCodes and roles when there are multiple declarations of the query parameter" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set("IT223100", "IT223101", "DK003102", "IT314102"))),
        equalTo(Some(Set("GB", "XI", "AW", "BL"))),
        equalTo(Some(Set("AUT", "CCA", "ACE", "RSS"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?referenceNumbers=IT223100,IT223101&referenceNumbers=DK003102,IT314102&countryCodes=GB,XI&countryCodes=AW,BL&roles=AUT,CCA&roles=ACE,RSS")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated reference numbers, countries and roles when there is no value declared for the query parameter" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(
      repository.fetchCustomsOfficeLists(
        equalTo(Some(Set.empty)),
        equalTo(Some(Set.empty)),
        equalTo(Some(Set.empty)),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices?referenceNumbers=&countryCodes=&roles=")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeLists(equalTo(None), equalTo(None), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 401 Unauthorized when the user's token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 403 Forbidden when the user's token cannot be validated" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 403, "message" -> "Forbidden")
    response.status mustBe Status.FORBIDDEN
  }

  it should "return 500 Internal Server Error when there is an error communicating with internal-auth" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/offices")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

}
