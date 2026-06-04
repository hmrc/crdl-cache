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
import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
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
import uk.gov.hmrc.crdlcache.repositories.StandardCodeListsRepository
import uk.gov.hmrc.crdlcache.repositories.CorrespondenceListsRepository
import uk.gov.hmrc.crdlcache.models.CodeListEntry
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC01, CL008, E200}



class CodeListEntriesV2ControllerSpec
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
  private val codeListsRepository = mock[StandardCodeListsRepository]
  private val correspondenceListsRepository = mock[CorrespondenceListsRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  override def beforeEach(): Unit = {
    reset(authStub)
    reset(codeListsRepository)
    reset(correspondenceListsRepository)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure{"play.http.router" -> "app.Routes"}
    .disable[InternalAuthModule]
    .overrides(
      bind[StubBehaviour].toInstance(authStub),
      bind[BackendAuthComponents].toProvider[BackendAuthStubProvider],
      bind[StandardCodeListsRepository].toInstance(codeListsRepository),
      bind[CorrespondenceListsRepository].toInstance(correspondenceListsRepository),
      bind[HttpClientV2].toInstance(httpClientV2),
      bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
    )
    .build()

    private val defaultPageNum = 1
    private val defaultPageSize = 10

    private val defaultNumOfEntries = 5
    private val standardEntryCodeList = BC01
    private val pdEntryCodeListCode = CL008
    private val correspondenceEntryCodeList = E200
    private val entryInstant = Instant.parse("2025-06-29T00:00:00Z")
    private val entryPhase = "P6"
    private val entryDomain = "NCTS"

    private val generateEntries = (code: CodeListCode, withPhaseAndDomain: Boolean) => Seq.tabulate(defaultNumOfEntries) {n =>
      CodeListEntry(
        code,
        s"${code.code}-key-$n",
        s"${code.code}-value-$n",
        entryInstant,
        None,
        None,
        Json.obj("status" -> "active"),
        if withPhaseAndDomain then Some(entryPhase) else None,
        if withPhaseAndDomain then Some(entryDomain) else None
      )
    }

    private val expectedPDEntryPagedResult = (entries: Seq[CodeListEntry]) => Json.obj(
    "items" -> entries.map(e => Json.obj("key" -> e.key, "value" -> e.value, "properties" -> e.properties)),
    "pageNum"     -> 1,
    "pageSize"    -> 10,
    "itemsInPage" -> 5,
    "totalItems"  -> 5,
    "totalPages"  -> 1
  )

    val configureAuthToPass = () =>
      when(authStub.stubAuth(equalTo(Some(ReadCodeLists)), equalTo(Retrieval.EmptyRetrieval)))
      .thenReturn(Future.unit)

    "CodeListEntriesV2Controller.fetchCodeListEntries" should "return 200 OK with a paged results when no filters are provided" in {
      val pdEntries = generateEntries(pdEntryCodeListCode, true)
      configureAuthToPass()
      when(codeListsRepository.fetchEntriesPaged(equalTo(pdEntryCodeListCode), equalTo(fixedInstant), equalTo(1), equalTo(10), equalTo(None), equalTo(None)))
        .thenReturn((Future.successful(pdEntries)))
      when(codeListsRepository.countEntries(equalTo(pdEntryCodeListCode), equalTo(fixedInstant), equalTo(None), equalTo(None)))
        .thenReturn(Future.successful(5L))

      val response = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$pdEntryCodeListCode/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

      response.status mustBe Status.OK
      response.json mustBe expectedPDEntryPagedResult(pdEntries)
    }

    it should "access the CodeListsRepository for a standard code list" in {
      val standardEntries = generateEntries(standardEntryCodeList, true)
      configureAuthToPass()
      when(codeListsRepository.fetchEntriesPaged(equalTo(standardEntryCodeList), equalTo(fixedInstant), equalTo(1), equalTo(10), equalTo(None), equalTo(None)))
        .thenReturn((Future.successful(standardEntries)))
      when(codeListsRepository.countEntries(equalTo(standardEntryCodeList), equalTo(fixedInstant), equalTo(None), equalTo(None)))
        .thenReturn(Future.successful(5L))

      val response = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

      response.status mustBe Status.OK
      verify(codeListsRepository, times(1)).fetchEntriesPaged(
        equalTo(standardEntryCodeList),
        equalTo(fixedInstant),
        equalTo(defaultPageNum),
        equalTo(defaultPageSize),
        equalTo(None),
        equalTo(None)
      )
      verify(correspondenceListsRepository, times(0)).fetchEntries(any(), any(), any(), any(), any(), any())
    }

    it should "access the CodeListsRepository for a Phase/Domain code list" in {
      val pdEntries = generateEntries(pdEntryCodeListCode, true)
      configureAuthToPass()
      when(codeListsRepository.fetchEntriesPaged(equalTo(pdEntryCodeListCode), equalTo(fixedInstant), equalTo(1), equalTo(10), equalTo(None), equalTo(None)))
        .thenReturn((Future.successful(pdEntries)))
      when(codeListsRepository.countEntries(equalTo(pdEntryCodeListCode), equalTo(fixedInstant), equalTo(None), equalTo(None)))
        .thenReturn(Future.successful(5L))

      val response = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$pdEntryCodeListCode/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

      response.status mustBe Status.OK
      verify(codeListsRepository, times(1)).fetchEntriesPaged(
        equalTo(pdEntryCodeListCode),
        equalTo(fixedInstant),
        equalTo(defaultPageNum),
        equalTo(defaultPageSize),
        equalTo(None),
        equalTo(None)
      )
      verify(correspondenceListsRepository, times(0)).fetchEntries(any(), any(), any(), any(), any(), any())
    }

    it should "access the CorrespondenceListsRepository for a Correspondence code list" in {
      val correspondenceEntries = generateEntries(correspondenceEntryCodeList, false)
      configureAuthToPass()
      when(correspondenceListsRepository.fetchEntriesPaged(equalTo(correspondenceEntryCodeList), equalTo(fixedInstant), equalTo(1), equalTo(10), equalTo(None), equalTo(None)))
        .thenReturn((Future.successful(correspondenceEntries)))
      when(correspondenceListsRepository.countEntries(equalTo(correspondenceEntryCodeList), equalTo(fixedInstant), equalTo(None), equalTo(None)))
        .thenReturn(Future.successful(5L))

      val response = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$correspondenceEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

      response.status mustBe Status.OK
      verify(correspondenceListsRepository, times(1)).fetchEntriesPaged(
        equalTo(correspondenceEntryCodeList),
        equalTo(fixedInstant),
        equalTo(defaultPageNum),
        equalTo(defaultPageSize),
        equalTo(None),
        equalTo(None)
      )
      verify(codeListsRepository, times(0)).fetchEntries(any(), any(), any(), any(), any(), any())
    }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    configureAuthToPass()
    when(codeListsRepository.fetchEntriesPaged(equalTo(standardEntryCodeList), equalTo(fixedInstant), equalTo(1), equalTo(10), equalTo(None), equalTo(None)))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val statusCode = try {
        httpClientV2.get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
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
    // Do not configureAuthToPass()

    val (statusCode, responseJson) = try {
      val r = httpClientV2
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
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
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
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
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
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
        .get(url"http://localhost:$port/crdl-cache/admin/lists/$standardEntryCodeList/entries?pageNum=$defaultPageNum&pageSize=$defaultPageSize")
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
