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

package uk.gov.hmrc.crdlcache.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08
import uk.gov.hmrc.crdlcache.models.dps.*
import uk.gov.hmrc.crdlcache.models.dps.RelationType.{Next, Prev, Self}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class DpsConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with EitherValues {

  given actorSystem: ActorSystem = ActorSystem("test")

  given mat: Materializer = Materializer(actorSystem)

  given hc: HeaderCarrier = HeaderCarrier()

  private val clientId     = "a0ce80bc-14b6-48eb-a8c0-96f1a927a573"
  private val clientSecret = "5a8e64bc855c4e6445cab63cee753bc1"

  // Basic auth header, produced by:
  // printf "$CLIENT_ID:$CLIENT_SECRET" | base64
  private val expectedEncodedAuthHeader =
    "Basic YTBjZTgwYmMtMTRiNi00OGViLWE4YzAtOTZmMWE5MjdhNTczOjVhOGU2NGJjODU1YzRlNjQ0NWNhYjYzY2VlNzUzYmMx"

  private val config = AppConfig(
    Configuration(
      "appName"                                    -> "crdl-cache",
      "microservice.services.dps-api.host"         -> "localhost",
      "microservice.services.dps-api.path"         -> "iv_crdl_reference_data",
      "microservice.services.dps-api.port"         -> wireMockPort,
      "microservice.services.dps-api.clientId"     -> clientId,
      "microservice.services.dps-api.clientSecret" -> clientSecret,
      "import-codelists.last-updated-date.default" -> "2025-05-29",
      "import-codelists.codelists"                 -> List.empty,
      "http-verbs.retries.intervals"               -> List("1.millis")
    )
  )

  private val connector =
    new DpsConnector(
      httpClientV2,
      config
    )

  private val codelistResponse = CodeListResponse(
    List(
      CodeListSnapshot(
        BC08,
        "Country",
        19,
        List(
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("AW")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("811")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Aruba"))
          ),
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("AX")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("812")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "ÅLAND ISLANDS"))
          ),
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("AZ")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("813")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Azerbaijan"))
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://localhost:9443/server/central_reference_data_library/ws_iv_crdl_reference_data/views/iv_crdl_reference_data?codelist_code=BC08"
      )
    )
  )

  private val snapshotsPage1 = CodeListResponse(
    List(
      CodeListSnapshot(
        BC08,
        "Country",
        1,
        List(
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("BL")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("823")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Saint Barthélemy"))
          ),
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("BM")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("824")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Bermuda"))
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://localhost:9443/server/central_reference_data_library/ws_iv_crdl_reference_data/views/iv_crdl_reference_data?%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      ),
      Relation(
        Next,
        "?%24start_index=10&%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      )
    )
  )

  private val snapshotsPage2 = CodeListResponse(
    List(
      CodeListSnapshot(
        BC08,
        "Country",
        12,
        List(
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("CX")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("848")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Christmas Island"))
          ),
          CodeListEntry(
            List(
              DataItem("CountryCode", Some("CY")),
              DataItem("Action_Operation", Some("U")),
              DataItem("Action_ActivationDate", Some("17-01-2024")),
              DataItem("Action_ActionIdentification", Some("849")),
              DataItem("Action_ResponsibleDataManager", None),
              DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Cyprus"))
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://localhost:9443/server/central_reference_data_library/ws_iv_crdl_reference_data/views/iv_crdl_reference_data?%24orderby=snapshotversion+ASC&code_list_code=BC08&%24start_index=10&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      ),
      Relation(
        Prev,
        "?%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      )
    )
  )

  override lazy val wireMockRootDirectory = "it/test/resources"
  private val uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

  "DpsConnector.fetchCodelist" should "return a codelist response when DPS API returns 200" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .willReturn(
          ok().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON).withBodyFile("BC08.json")
        )
    )
    connector.fetchCodelist(BC08).map { result =>
      result.value mustBe codelistResponse
    }
  }

  it should "return an UpstreamErrorResponse when the server returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )
    connector.fetchCodelist(BC08).map { result =>
      result.left.value mustBe a[UpstreamErrorResponse]
    }
  }

  it should "return an UpstreamErrorResponse when the server returns a server error" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )
    connector.fetchCodelist(BC08).map { result =>
      result.left.value mustBe a[UpstreamErrorResponse]
    }
  }

  "DPSConnector.fetchCodelistSnapshots" should "produce a CodeListResponse for each page of codelist snapshots" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page3.json")
        )
    )

    connector
      .fetchCodelistSnapshots(BC08, lastUpdatedDate)
      .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
      .map(_ mustBe List(snapshotsPage1, snapshotsPage2))
  }

  it should "throw UpstreamErrorResponse when there is a client error in the first page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodelistSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the first page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodelistSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a client error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page1.json")
        )
    )

    // Page 2 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodelistSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page1.json")
        )
    )

    // Page 2 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodelistSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "not retry when there is a client error while fetching a page" in {
    val retryScenario   = "Retry"
    val failedState     = "Failed"
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    // Page 1 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
        .willSetStateTo(failedState)
    )

    // Page 1 (Retry, OK) - it would succeed if it did retry, but it shouldn't do that!
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page1.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodelistSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "retry when there is a server error while fetching a page" in {
    val retryScenario   = "Retry"
    val failedState     = "Failed"
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

    // Page 1 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
        .willSetStateTo(failedState)
    )

    // Page 1 (Retry, OK)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("BC08_snapshots_page3.json")
        )
    )

    connector
      .fetchCodelistSnapshots(BC08, lastUpdatedDate)
      .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
      .map(_ mustBe List(snapshotsPage1, snapshotsPage2))
  }
}
