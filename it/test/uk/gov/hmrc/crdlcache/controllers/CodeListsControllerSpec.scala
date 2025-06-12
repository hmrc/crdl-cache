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
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.crdlcache.repositories.CodeListsRepository
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

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

  private val repository = mock[CodeListsRepository]

  private val fixedInstant = Instant.parse("2025-06-05T00:00:00Z")

  private val entries = List(
    CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "811"
      )
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
      )
    )
  )

  override def beforeEach(): Unit = {
    reset(repository)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[CodeListsRepository].toInstance(repository),
        bind[HttpClientV2].toInstance(httpClientV2),
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC))
      )
      .build()

  "CodeListsController" should "return 200 OK when there are no errors" in {
    when(repository.fetchCodeListEntries(equalTo(BC08), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(entries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
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

  it should "return 200 OK when there are no entries to return" in {
    when(repository.fetchCodeListEntries(equalTo(BC08), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr()
    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from a query parameter when there is only one key" in {
    when(
      repository.fetchCodeListEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from a query parameter when there are multiple keys" in {
    when(
      repository.fetchCodeListEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there are multiple declarations of the query parameter" in {
    when(
      repository.fetchCodeListEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI", "AW", "BL"))),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI&keys=AW,BL")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there is no value declared for the query parameter" in {
    when(
      repository.fetchCodeListEntries(
        equalTo(BC08),
        equalTo(Some(Set.empty)),
        equalTo(fixedInstant)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "return 400 Bad Request when the user provides an invalid codelist code" in {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/CL999")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "return 400 Bad Request when the user provides an invalid activeAt timestamp" in {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?activeAt=2025-06-05")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 400, "message" -> "bad request, cause: REDACTED")
    response.status mustBe Status.BAD_REQUEST
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    when(repository.fetchCodeListEntries(equalTo(BC08), equalTo(None), equalTo(fixedInstant)))
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }
}
