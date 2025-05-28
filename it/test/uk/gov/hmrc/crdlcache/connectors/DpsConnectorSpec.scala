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

import com.github.tomakehurst.wiremock.client.WireMock.{ok, *}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08
import uk.gov.hmrc.crdlcache.models.dps.RelationType.{Next, Prev, Self}
import uk.gov.hmrc.crdlcache.models.dps.*
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{LocalDate, ZoneOffset}
import java.util.concurrent.ConcurrentLinkedDeque
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class DpsConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with EitherValues {

  given actorSystem: ActorSystem = ActorSystem("test")

  given mat: Materializer = Materializer(actorSystem)

  given hc: HeaderCarrier = HeaderCarrier()

  val clientId     = "a0ce80bc-14b6-48eb-a8c0-96f1a927a573"
  val clientSecret = "5a8e64bc855c4e6445cab63cee753bc1"
  val expectedEncodedAuthHeader =
    "Basic YTBjZTgwYmMtMTRiNi00OGViLWE4YzAtOTZmMWE5MjdhNTczOjVhOGU2NGJjODU1YzRlNjQ0NWNhYjYzY2VlNzUzYmMx"
  val config = AppConfig(
    Configuration(
      "appName"                                    -> "crdl-cache",
      "microservice.services.dps-api.host"         -> "localhost",
      "microservice.services.dps-api.path"         -> "iv_crdl_reference_data",
      "microservice.services.dps-api.port"         -> wireMockPort,
      "microservice.services.dps-api.clientId"     -> clientId,
      "microservice.services.dps-api.clientSecret" -> clientSecret
    )
  )
  val connector =
    new DpsConnector(
      httpClientV2,
      config
    )

  val codelistResponse = CodeListResponse(
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

  val snapshotsPage1: CodeListResponse = CodeListResponse(
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

  val snapshotsPage2: CodeListResponse = CodeListResponse(
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
  val uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

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

  "DPSConnector.fetchCodelistSnapshots" should "call the processResponse function for each page of snapshots" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

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

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
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

    val responses = new ConcurrentLinkedDeque[CodeListResponse]()

    connector
      .fetchCodelistSnapshots(BC08, lastUpdatedDate) { response =>
        Future.successful(responses.addLast(response))
      }
      .map { _ =>
        responses.asScala.toList mustBe List(snapshotsPage1, snapshotsPage2)
      }
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
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate)(_ => Future.unit)
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
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate)(_ => Future.unit)
    }
  }

  it should "rethrow the underlying exception when there is an error while processing the first page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

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

    recoverToSucceededIf[RuntimeException] {
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate)(_ =>
        Future.failed(new RuntimeException("Oh no!"))
      )
    }
  }

  it should "throw UpstreamErrorResponse when there is a client error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

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
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate)(_ => Future.unit)
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

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
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate)(_ => Future.unit)
    }
  }

  it should "rethrow the underlying exception when there is an error while processing the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC)

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

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
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

    val responses = new ConcurrentLinkedDeque[CodeListResponse]()

    recoverToSucceededIf[RuntimeException] {
      connector.fetchCodelistSnapshots(BC08, lastUpdatedDate) { response =>
        if (response.links.exists(_.rel == Next))
          Future.successful(responses.addLast(response))
        else {
          // Throw an exception on the final page
          Future.failed(new RuntimeException("Oh no!"))
        }
      }
    }.map { _ =>
      // We should still receive the initial page
      responses.asScala.toList mustBe List(snapshotsPage1)
    }
  }
}
