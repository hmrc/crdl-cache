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
import RelationType.{Next, Prev, Self}
import uk.gov.hmrc.crdlcache.models.dps.codeList.{
  CodeListEntry,
  CodeListResponse,
  CodeListSnapshot,
  DataItem,
  LanguageDescription
}
import uk.gov.hmrc.crdlcache.models.dps.col.{
  CustomsOfficeDetail,
  CustomsOffice,
  CustomsOfficeListResponse,
  CustomsOfficeTimetable,
  RDEntryStatus,
  RoleTrafficCompetence,
  SpecificNotes,
  TimetableLine
}
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

  given Materializer = Materializer(actorSystem)

  given HeaderCarrier = HeaderCarrier()

  private val clientId     = "a0ce80bc-14b6-48eb-a8c0-96f1a927a573"
  private val clientSecret = "5a8e64bc855c4e6445cab63cee753bc1"

  // Basic auth header, produced by:
  // printf "$CLIENT_ID:$CLIENT_SECRET" | base64
  private val expectedEncodedAuthHeader =
    "Basic YTBjZTgwYmMtMTRiNi00OGViLWE4YzAtOTZmMWE5MjdhNTczOjVhOGU2NGJjODU1YzRlNjQ0NWNhYjYzY2VlNzUzYmMx"

  private val config = AppConfig(
    Configuration(
      "appName"                                            -> "crdl-cache",
      "microservice.services.dps-api.host"                 -> "localhost",
      "microservice.services.dps-api.ref-data-path"        -> "iv_crdl_reference_data",
      "microservice.services.dps-api.customs-offices-path" -> "iv_crdl_customs_office",
      "microservice.services.dps-api.port"                 -> wireMockPort,
      "microservice.services.dps-api.clientId"             -> clientId,
      "microservice.services.dps-api.clientSecret"         -> clientSecret,
      "import-codelists.schedule"                          -> "* * * * * ?",
      "import-offices.schedule"                            -> "* * * * * ?",
      "import-codelists.last-updated-date.default"         -> "2025-05-29",
      "import-codelists.codelists"                         -> List.empty,
      "http-verbs.retries.intervals"                       -> List("1.millis")
    )
  )

  private val connector =
    new DpsConnector(
      httpClientV2,
      config
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

  private val customsOfficeListPage1 = CustomsOfficeListResponse(
    List(
      CustomsOffice(
        RDEntryStatus("valid", "01-05-2025"),
        "IT223100",
        None,
        Some("ITP00002"),
        None,
        None,
        None,
        "IT",
        Some("test@test.it"),
        None,
        Some("20250501"),
        None,
        "40121",
        "0039 435345",
        Some("0039 435345"),
        None,
        Some("Q"),
        None,
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          CustomsOfficeDetail(
            "EMILIA 1 BOLOGNA",
            "IT",
            "BOLOGNA",
            "0",
            Some("A"),
            "1",
            "VIALE PIETRAMELLARA, 1/2"
          )
        ),
        CustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20180101",
          "20991231",
          List(
            TimetableLine(
              "1",
              "0800",
              "1800",
              "6",
              None,
              None,
              List(
                RoleTrafficCompetence("EXC", "R"),
                RoleTrafficCompetence("REG", "N/A"),
                RoleTrafficCompetence("SCO", "N/A"),
                RoleTrafficCompetence("PLA", "N/A"),
                RoleTrafficCompetence("DIS", "N/A"),
                RoleTrafficCompetence("RFC", "N/A"),
                RoleTrafficCompetence("EXT", "N/A"),
                RoleTrafficCompetence("EXP", "N/A"),
                RoleTrafficCompetence("IPR", "N/A")
              )
            )
          )
        )
      ),
      CustomsOffice(
        RDEntryStatus("valid", "01-05-2025"),
        "IT223101",
        None,
        Some("ITP00002"),
        Some("IT223101"),
        Some("IT223101"),
        None,
        "IT",
        Some("test@it"),
        None,
        Some("20250501"),
        None,
        "40131",
        "1234 045483382",
        Some("2343 34543"),
        None,
        Some("Q"),
        None,
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          CustomsOfficeDetail(
            "AEROPORTO DI BOLOGNA",
            "IT",
            "BOLOGNA",
            "0",
            Some("A"),
            "1",
            "VIA DELL'AEROPORTO, 1"
          )
        ),
        CustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20180101",
          "20991231",
          List(
            TimetableLine(
              "1",
              "0000",
              "2359",
              "6",
              None,
              None,
              List(
                RoleTrafficCompetence("DEP", "AIR"),
                RoleTrafficCompetence("INC", "AIR"),
                RoleTrafficCompetence("TXT", "AIR"),
                RoleTrafficCompetence("DES", "AIR"),
                RoleTrafficCompetence("ENQ", "N/A"),
                RoleTrafficCompetence("ENT", "AIR"),
                RoleTrafficCompetence("EXC", "N/A"),
                RoleTrafficCompetence("EXP", "AIR"),
                RoleTrafficCompetence("EXT", "AIR"),
                RoleTrafficCompetence("REC", "N/A"),
                RoleTrafficCompetence("REG", "N/A"),
                RoleTrafficCompetence("TRA", "AIR"),
                RoleTrafficCompetence("EIN", "AIR"),
                RoleTrafficCompetence("PLA", "N/A"),
                RoleTrafficCompetence("DIS", "N/A"),
                RoleTrafficCompetence("RFC", "N/A"),
                RoleTrafficCompetence("IPR", "N/A")
              )
            )
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://vdp.nonprod.denodo.hip.ns2n.corp.hmrc.gov.uk:9443/server/central_reference_data_library/ws_iv_crdl_customs_office/views/iv_crdl_customs_office"
      ),
      Relation(
        Next,
        "?%24start_index=10&%24count=10"
      )
    )
  )

  private val customsOfficeListPage2 = CustomsOfficeListResponse(
    List(
      CustomsOffice(
        RDEntryStatus("valid", "22-03-2025"),
        "DK003102",
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
        "0",
        None,
        None,
        List(SpecificNotes("SN0009")),
        List(
          CustomsOfficeDetail(
            "Hirtshals Toldekspedition",
            "DA",
            "Hirtshals",
            "0",
            None,
            "0",
            "Dalsagervej 7"
          )
        ),
        CustomsOfficeTimetable(
          "1",
          None,
          "20180101",
          "20991231",
          List(
            TimetableLine(
              "1",
              "0800",
              "1600",
              "5",
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
      ),
      CustomsOffice(
        RDEntryStatus("valid", "22-03-2025"),
        "IT314102",
        None,
        Some("ITP00023"),
        Some("IT314102"),
        Some("IT314102"),
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
        "0",
        Some("IT"),
        Some("TIN"),
        List.empty,
        List(
          CustomsOfficeDetail(
            "ORBASSANO",
            "IT",
            "ORBASSANO (TO)",
            "0",
            Some("A"),
            "1",
            "Prima Strada, 5"
          )
        ),
        CustomsOfficeTimetable(
          "1",
          Some("ALL YEAR"),
          "20240101",
          "99991231",
          List(
            TimetableLine(
              "1",
              "0800",
              "1800",
              "5",
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
    ),
    List(
      Relation(
        Self,
        "https://vdp.nonprod.denodo.hip.ns2n.corp.hmrc.gov.uk:9443/server/central_reference_data_library/ws_iv_crdl_customs_office/views/iv_crdl_customs_office?%24start_index=40"
      ),
      Relation(
        Prev,
        "?%24start_index=30&%24count=10"
      ),
      Relation(
        Next,
        "?%24start_index=50&%24count=10"
      )
    )
  )

  override lazy val wireMockRootDirectory = "it/test/resources"
  private val uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

  "DPSConnector.fetchCodelistSnapshots" should "produce a CodeListResponse for each page of codelist snapshots" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page3.json")
        )
    )

    connector
      .fetchCodeListSnapshots(BC08, lastUpdatedDate)
      .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
      .map(_ mustBe List(snapshotsPage1, snapshotsPage2))
  }

  it should "throw UpstreamErrorResponse when there is a client error in the first page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .fetchCodeListSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the first page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .fetchCodeListSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a client error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page1.json")
        )
    )

    // Page 2 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .fetchCodeListSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the second page" in {
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page1.json")
        )
    )

    // Page 2 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .fetchCodeListSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "not retry when there is a client error while fetching a page" in {
    val retryScenario   = "Retry"
    val failedState     = "Failed"
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    // Page 1 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page1.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCodeListSnapshots(BC08, lastUpdatedDate)
        .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
    }
  }

  it should "retry when there is a server error while fetching a page" in {
    val retryScenario   = "Retry"
    val failedState     = "Failed"
    val lastUpdatedDate = LocalDate.of(2025, 5, 28).atStartOfDay(ZoneOffset.UTC).toInstant

    // Page 1 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
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
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .withQueryParam("last_updated_date", equalTo("2025-05-28T00:00:00Z"))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .withQueryParam("$orderby", equalTo("snapshotversion ASC"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC08_snapshots_page3.json")
        )
    )

    connector
      .fetchCodeListSnapshots(BC08, lastUpdatedDate)
      .runWith(Sink.collection[CodeListResponse, List[CodeListResponse]])
      .map(_ mustBe List(snapshotsPage1, snapshotsPage2))
  }

  "DPSConnector.fetchCustomsOfficeLists" should "produce a CustomOfficeListResponse for each page of custom office list" in {
    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page3.json")
        )
    )

    connector.fetchCustomsOfficeLists
      .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
      .map(_ mustBe List(customsOfficeListPage1, customsOfficeListPage2))
  }

  it should "throw UpstreamErrorResponse when there is a client error in the first page" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.
        fetchCustomsOfficeLists
        .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the first page" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCustomsOfficeLists
        .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a client error in the second page" in {
    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page1.json")
        )
    )

    // Page 2 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCustomsOfficeLists
        .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
    }
  }

  it should "throw UpstreamErrorResponse when there is a server error in the second page" in {
    // Page 1
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page1.json")
        )
    )

    // Page 2 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCustomsOfficeLists
        .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
    }
  }

  it should "not retry when there is a client error while fetching a page" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    // Page 1 (Bad Request)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          badRequest().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
        .willSetStateTo(failedState)
    )

    // Page 1 (Retry, OK) - it would succeed if it did retry, but it shouldn't do that!
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page1.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector
        .fetchCustomsOfficeLists
        .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
    }
  }

  it should "retry when there is a server error while fetching a page" in {
    val retryScenario   = "Retry"
    val failedState     = "Failed"

    // Page 1 (Internal Server Error)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          serverError().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
        .willSetStateTo(failedState)
    )

    // Page 1 (Retry, OK)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("0"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page1.json")
        )
    )

    // Page 2
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("10"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page2.json")
        )
    )

    // Page 3 (empty)
    stubFor(
      get(urlPathEqualTo("/iv_crdl_customs_office"))
        .withHeader("correlationId", matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("$start_index", equalTo("20"))
        .withQueryParam("$count", equalTo("10"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/COL_page3.json")
        )
    )

    connector
      .fetchCustomsOfficeLists
      .runWith(Sink.collection[CustomsOfficeListResponse, List[CustomsOfficeListResponse]])
      .map(_ mustBe List(customsOfficeListPage1, customsOfficeListPage2))
  }
}
