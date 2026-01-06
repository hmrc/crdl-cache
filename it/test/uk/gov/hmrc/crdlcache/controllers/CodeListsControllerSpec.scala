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

import org.mockito.ArgumentMatchers.eq as equalTo
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCodeLists
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC36, BC66, E200, CL231}
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListEntry, LastUpdated}
import uk.gov.hmrc.crdlcache.repositories.{
  CorrespondenceListsRepository,
  LastUpdatedRepository,
  StandardCodeListsRepository
}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.modules.InternalAuthModule
import uk.gov.hmrc.internalauth.client.test.StubBehaviour
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class CodeListsControllerSpec
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

  private val authStub                      = mock[StubBehaviour]
  private val codeListsRepository           = mock[StandardCodeListsRepository]
  private val correspondenceListsRepository = mock[CorrespondenceListsRepository]
  private val lastUpdatedRepository         = mock[LastUpdatedRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  private val codeListEntries = List(
    CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "811"
      ),
      None,
      None
    ),
    CodeListEntry(
      BC08,
      "BL",
      "Saint Barthélemy",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "823"
      ),
      None,
      None
    )
  )

  private val correspondenceListEntries = List(
    CodeListEntry(
      E200,
      "27101944",
      "E430",
      Instant.parse("2025-01-01T00:00:00Z"),
      None,
      Some(Instant.parse("2024-12-30T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "433"
      ),
      None,
      None
    ),
    CodeListEntry(
      E200,
      "27101944",
      "E440",
      Instant.parse("2025-01-01T00:00:00Z"),
      None,
      Some(Instant.parse("2024-12-30T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "437"
      ),
      None,
      None
    )
  )

  private val phaseAndDomainListEntries = List(
    CodeListEntry(
      CL231,
      "T1",
      "Goods not having the customs status of Union goods, which are placed under the common transit procedure.",
      Instant.parse("2025-01-23T00:00:00Z"),
      None,
      Some(Instant.parse("2024-12-30T00:00:00Z")),
      Json.obj(
        "state" -> "valid"
      ),
      Some("6"),
      Some("NCTS")
    ),
    CodeListEntry(
      CL231,
      "TIR",
      "TIR carnet",
      Instant.parse("2025-01-23T00:00:00Z"),
      None,
      Some(Instant.parse("2024-12-30T00:00:00Z")),
      Json.obj(
        "state" -> "valid"
      ),
      Some("6"),
      Some("NCTS")
    )
  )

  private val lastUpdatedEntries = List(
    LastUpdated(BC08, 1, Instant.parse("2025-06-29T00:00:00Z")),
    LastUpdated(BC66, 1, Instant.parse("2025-06-28T00:00:00Z"))
  )

  override def beforeEach(): Unit = {
    reset(authStub)
    reset(codeListsRepository)
    reset(lastUpdatedRepository)
    reset(correspondenceListsRepository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[InternalAuthModule]
      .overrides(
        bind[StubBehaviour].toInstance(authStub),
        bind[BackendAuthComponents].toProvider[BackendAuthStubProvider],
        bind[StandardCodeListsRepository].toInstance(codeListsRepository),
        bind[CorrespondenceListsRepository].toInstance(correspondenceListsRepository),
        bind[LastUpdatedRepository].toInstance(lastUpdatedRepository),
        bind[HttpClientV2].toInstance(httpClientV2),
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
      )
      .build()

  "CodeListsController" should "return 200 OK when there are no errors" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(codeListEntries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr(
      Json.obj(
        "key"        -> "AW",
        "value"      -> "Aruba",
        "properties" -> Json.obj("actionIdentification" -> "811")
      ),
      Json.obj(
        "key"        -> "BL",
        "value"      -> "Saint Barthélemy",
        "properties" -> Json.obj("actionIdentification" -> "823")
      )
    )

    response.status mustBe Status.OK
  }

  it should "use the correct repository for correspondence lists like E200" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      correspondenceListsRepository.fetchEntries(
        equalTo(E200),
        equalTo(None),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(correspondenceListEntries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${E200.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr(
      Json.obj(
        "key"        -> "27101944",
        "value"      -> "E430",
        "properties" -> Json.obj("actionIdentification" -> "433")
      ),
      Json.obj(
        "key"        -> "27101944",
        "value"      -> "E440",
        "properties" -> Json.obj("actionIdentification" -> "437")
      )
    )

    response.status mustBe Status.OK
  }

  it should "use the correct repository for phase and domain lists like CL231" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(CL231),
        equalTo(None),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(phaseAndDomainListEntries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${CL231.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    println(response.json)

    response.json mustBe Json.arr(
      Json.obj(
        "key"        -> "T1",
        "value"      -> "Goods not having the customs status of Union goods, which are placed under the common transit procedure.",
        "properties" -> Json.obj("state" -> "valid"),
      ),
      Json.obj(
        "key"        -> "TIR",
        "value"      -> "TIR carnet",
        "properties" -> Json.obj("state" -> "valid")
      )
    )

    response.status mustBe Status.OK
  }

  it should "return 200 OK when there are no entries to return" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr()
    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from the keys parameter when there is only one key" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB"))),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from the keys parameter when there are multiple keys" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI"))),
        equalTo(None),
        equalTo(fixedInstant)
    ))
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there are multiple declarations of the keys parameter" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI", "AW", "BL"))),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI&keys=AW,BL")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there is no value declared for the keys parameter" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set.empty)),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as boolean property filters when they are valid boolean values" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC36),
        equalTo(Some(Set("B000"))),
        equalTo(Some(Map("alcoholicStrengthApplicabilityFlag" -> JsBoolean(true)))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC36.code}?keys=B000&alcoholicStrengthApplicabilityFlag=true"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as null property filters when the query parameter value is null" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC66),
        equalTo(Some(Set("B"))),
        equalTo(Some(Map("responsibleDataManager" -> JsNull))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC66.code}?keys=B&responsibleDataManager=null"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as String property filters when they are neither boolean nor null values" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB"))),
        equalTo(Some(Map("actionIdentification" -> JsString("384")))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB&actionIdentification=384"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/BC08")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 401 Unauthorized when the user's token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/BC08")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 403 Forbidden when the user's token cannot be validated" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/BC08")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 403, "message" -> "Forbidden")
    response.status mustBe Status.FORBIDDEN
  }

  it should "return 400 Bad Request when the user provides an invalid codelist code" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/CL999")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "return 400 Bad Request when the user provides an invalid activeAt timestamp" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?activeAt=2025-06-05")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

  it should "return 500 Internal Server Error when there is an error communicating with internal-auth" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

  "CodeListsController.fetchCodeListVersions" should "return 200 OK when there are no errors" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(lastUpdatedRepository.fetchAllLastUpdated)
      .thenReturn(Future.successful(lastUpdatedEntries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr(
      Json.obj(
        "codeListCode"    -> "BC08",
        "snapshotVersion" -> 1,
        "lastUpdated"     -> "2025-06-29T00:00:00Z"
      ),
      Json.obj(
        "codeListCode"    -> "BC66",
        "snapshotVersion" -> 1,
        "lastUpdated"     -> "2025-06-28T00:00:00Z"
      )
    )

    response.status mustBe Status.OK
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 401 Unauthorized when the user's token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 403 Forbidden when the user's token cannot be validated" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 403, "message" -> "Forbidden")
    response.status mustBe Status.FORBIDDEN
  }

  it should "return 500 Internal Server Error when there is an error fetching from the last updated repository" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    when(lastUpdatedRepository.fetchAllLastUpdated)
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

  it should "return 500 Internal Server Error when there is an error communicating with internal-auth" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500)))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }
}
