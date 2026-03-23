/*
 * Copyright 2026 HM Revenue & Customs
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

import org.apache.pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
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
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCodeLists
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC66, CL251}
import uk.gov.hmrc.crdlcache.models.LastUpdated
import uk.gov.hmrc.crdlcache.repositories.LastUpdatedRepository
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.modules.InternalAuthModule
import uk.gov.hmrc.internalauth.client.test.StubBehaviour
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class CodeListsV2ControllerSpec
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

  private val authStub              = mock[StubBehaviour]
  private val lastUpdatedRepository = mock[LastUpdatedRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  override def beforeEach(): Unit = {
    reset(authStub)
    reset(lastUpdatedRepository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("play.http.router" -> "app.v2.Routes")
      .disable[InternalAuthModule]
      .overrides(
        bind[StubBehaviour].toInstance(authStub),
        bind[BackendAuthComponents].toProvider[BackendAuthStubProvider],
        bind[LastUpdatedRepository].toInstance(lastUpdatedRepository),
        bind[HttpClientV2].toInstance(httpClientV2),
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
      )
      .build()

  private val lastUpdatedEntries = Seq(
    LastUpdated(BC08, 1, None, None, Instant.parse("2025-06-29T00:00:00Z")),
    LastUpdated(BC66, 1, None, None, Instant.parse("2025-06-28T00:00:00Z"))
  )

  private val pagedResultJson = Json.obj(
    "items" -> Json.arr(
      Json.obj("codeListCode" -> "BC08", "snapshotVersion" -> 1, "lastUpdated" -> "2025-06-29T00:00:00Z"),
      Json.obj("codeListCode" -> "BC66", "snapshotVersion" -> 1, "lastUpdated" -> "2025-06-28T00:00:00Z")
    ),
    "pageNum"     -> 1,
    "pageSize"    -> 10,
    "itemsInPage" -> 2,
    "totalItems"  -> 2,
    "totalPages"  -> 1
  )

  "CodeListsV2Controller.fetchCodeListVersions" should "return 200 OK with a paged result when no filters are provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(lastUpdatedEntries))
    when(lastUpdatedRepository.codeListCount(equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(2L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
    response.json mustBe pagedResultJson
  }

  it should "return 200 OK with an empty items list when there are no matching code lists" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(Seq.empty))
    when(lastUpdatedRepository.codeListCount(equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(0L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
    response.json mustBe Json.obj(
      "items"       -> Json.arr(),
      "pageNum"     -> 1,
      "pageSize"    -> 10,
      "itemsInPage" -> 0,
      "totalItems"  -> 0,
      "totalPages"  -> 0
    )
  }

  it should "return 200 OK when a codeListCode filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(Some("BC08")), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(Seq(LastUpdated(BC08, 1, None, None, Instant.parse("2025-06-29T00:00:00Z")))))
    when(lastUpdatedRepository.codeListCount(equalTo(Some("BC08")), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10&codeListCode=BC08")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when a phase filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(None), equalTo(Some("P6")), equalTo(None)))
      .thenReturn(Future.successful(Seq(LastUpdated(CL251, 1, Some("P6"), Some("NCTS"), Instant.parse("2025-06-29T00:00:00Z")))))
    when(lastUpdatedRepository.codeListCount(equalTo(None), equalTo(Some("P6")), equalTo(None)))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10&phase=P6")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when a domain filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(None), equalTo(None), equalTo(Some("NCTS"))))
      .thenReturn(Future.successful(Seq(LastUpdated(CL251, 1, Some("P6"), Some("NCTS"), Instant.parse("2025-06-29T00:00:00Z")))))
    when(lastUpdatedRepository.codeListCount(equalTo(None), equalTo(None), equalTo(Some("NCTS"))))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10&domain=NCTS")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when all three filters are provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(Some("CL251")), equalTo(Some("P6")), equalTo(Some("NCTS"))))
      .thenReturn(Future.successful(Seq(LastUpdated(CL251, 1, Some("P6"), Some("NCTS"), Instant.parse("2025-06-29T00:00:00Z")))))
    when(lastUpdatedRepository.codeListCount(equalTo(Some("CL251")), equalTo(Some("P6")), equalTo(Some("NCTS"))))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10&codeListCode=CL251&phase=P6&domain=NCTS")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchAllLastUpdatedV2(equalTo(1), equalTo(10), equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))
    when(lastUpdatedRepository.codeListCount(equalTo(None), equalTo(None), equalTo(None)))
      .thenReturn(Future.successful(0L))

    val statusCode = try {
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
        .status
    } catch {
      case e: UpstreamErrorResponse => e.statusCode
    }

    statusCode mustBe Status.INTERNAL_SERVER_ERROR
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
        .execute[HttpResponse]
        .futureValue
      (r.status, r.json)
    } catch {
      case e: UpstreamErrorResponse => (e.statusCode, Json.parse(e.message))
    }

    responseJson mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    statusCode mustBe Status.UNAUTHORIZED
  }

  it should "return 401 Unauthorized when the user's token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
      (r.status, r.json)
    } catch {
      case e: UpstreamErrorResponse => (e.statusCode, Json.parse(e.message))
    }

    responseJson mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    statusCode mustBe Status.UNAUTHORIZED
  }

  it should "return 403 Forbidden when the user's token cannot be validated" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)))

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
      (r.status, r.json)
    } catch {
      case e: UpstreamErrorResponse => (e.statusCode, Json.parse(e.message))
    }

    responseJson mustBe Json.obj("statusCode" -> 403, "message" -> "Forbidden")
    statusCode mustBe Status.FORBIDDEN
  }

  it should "return 500 Internal Server Error when there is an error communicating with internal-auth" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500)))

    val statusCode = try {
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/lists?pageNum=1&pageSize=10")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
        .status
    } catch {
      case e: UpstreamErrorResponse => e.statusCode
    }

    statusCode mustBe Status.INTERNAL_SERVER_ERROR
  }

  "CodeListsV2Controller.getSnapShot" should "return 200 OK with a paged result when no filters are provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchLastUpdated(equalTo(BC08)))
      .thenReturn(Future.successful(Some(LastUpdated(BC08, 1, None, None, Instant.parse("2025-06-29T00:00:00Z")))))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/snapshot/BC08")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
    response.json mustBe Json.obj("codeListCode" -> "BC08", "snapshotVersion" -> 1, "lastUpdated" -> "2025-06-29T00:00:00Z")
  }

  "CodeListsV2Controller.getSnapShot" should "return 404 Not Found when no snapshot is found" in {
    when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(lastUpdatedRepository.fetchLastUpdated(equalTo(BC08)))
      .thenReturn(Future.successful(None))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/snapshot/BC08")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.NOT_FOUND
  }
}
