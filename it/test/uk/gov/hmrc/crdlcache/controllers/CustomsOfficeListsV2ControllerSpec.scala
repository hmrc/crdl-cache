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
import org.mockito.ArgumentMatchers.{eq as equalTo}
import uk.gov.hmrc.crdlcache.models.CustomsOfficeSummary
import org.mockito.Mockito.{reset, when}
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCustomsOfficeLists
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}
import uk.gov.hmrc.internalauth.client.modules.InternalAuthModule
import uk.gov.hmrc.internalauth.client.test.StubBehaviour

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class CustomsOfficeListsV2ControllerSpec
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

  private val authStub   = mock[StubBehaviour]
  private val repository = mock[CustomsOfficeListsRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  override def beforeEach(): Unit = {
    reset(authStub)
    reset(repository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("play.http.router" -> "app.v2.Routes")
      .disable[InternalAuthModule]
      .overrides(
        bind[StubBehaviour].toInstance(authStub),
        bind[BackendAuthComponents].toProvider[BackendAuthStubProvider],
        bind[CustomsOfficeListsRepository].toInstance(repository),
        bind[HttpClientV2].toInstance(httpClientV2),
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
      )
      .build()

  private val summary = CustomsOfficeSummary("GB000060", "GB", "Dover")

  private val summaryJson = Json.obj(
    "referenceNumber"       -> "GB000060",
    "countryCode"           -> "GB",
    "customsOfficeUsualName" -> "Dover"
  )

  private val pagedResultJson = Json.obj(
    "items"       -> Json.arr(summaryJson),
    "pageNum"     -> 1,
    "pageSize"    -> 10,
    "itemsInPage" -> 1,
    "totalItems"  -> 1,
    "totalPages"  -> 1
  )

  "CustomsOfficeListsV2Controller" should "return 200 OK with a paged result when there are no filters" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
    response.json mustBe pagedResultJson
  }

  it should "return 200 OK with an empty items list when there are no matching summaries" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(Seq.empty))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(0L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
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

  it should "return 200 OK when an activeAt timestamp is provided" in {
    val activeAt = Instant.parse("2024-01-01T00:00:00Z")
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(activeAt), equalTo(1), equalTo(10),
      equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(activeAt), equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&activeAt=2024-01-01T00:00:00Z")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when a referenceNumber filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(Some("GB000060")), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(Some("GB000060")), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&referenceNumber=GB000060")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when a countryCode filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(None), equalTo(Some("GB")), equalTo(None)
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(None), equalTo(Some("GB")), equalTo(None)
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&countryCode=GB")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when an officeName filter is provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(None), equalTo(None), equalTo(Some("Dover"))
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(None), equalTo(None), equalTo(Some("Dover"))
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&officeName=Dover")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 200 OK when all three filters are provided" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(Some("GB000060")), equalTo(Some("GB")), equalTo(Some("Dover"))
    ))
      .thenReturn(Future.successful(Seq(summary)))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(Some("GB000060")), equalTo(Some("GB")), equalTo(Some("Dover"))
    ))
      .thenReturn(Future.successful(1L))

    val response = httpClientV2
      .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&referenceNumber=GB000060&countryCode=GB&officeName=Dover")
      .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
      .execute[HttpResponse]
      .futureValue

    response.status mustBe Status.OK
  }

  it should "return 400 Bad Request when activeAt is not a valid Instant" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val statusCode = try {
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10&activeAt=not-a-timestamp")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
        .status
    } catch {
      case e: UpstreamErrorResponse => e.statusCode
    }

    statusCode mustBe Status.BAD_REQUEST
  }

  it should "return 500 Internal Server Error when there is an error fetching summaries from the repository" in {
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)
    when(repository.fetchCustomsOfficeSummaries(
      equalTo(fixedInstant), equalTo(1), equalTo(10),
      equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))
    when(repository.customsOfficesCount(
      equalTo(fixedInstant), equalTo(None), equalTo(None), equalTo(None)
    ))
      .thenReturn(Future.successful(0L))

    val statusCode = try {
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
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
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
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
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
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
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)))

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
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
    when(authStub.stubAuth(equalTo(Some(ReadCustomsOfficeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500)))

    val statusCode = try {
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/v2/offices/summaries?pageNum=1&pageSize=10")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue
        .status
    } catch {
      case e: UpstreamErrorResponse => e.statusCode
    }

    statusCode mustBe Status.INTERNAL_SERVER_ERROR
  }
}
