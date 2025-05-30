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

import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08
import uk.gov.hmrc.crdlcache.models.dps.{CodeListEntry, CodeListResponse, CodeListSnapshot, DataItem, LanguageDescription}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

class DpsConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with EitherValues {
  val clientId = "a0ce80bc-14b6-48eb-a8c0-96f1a927a573"
  val clientSecret = "5a8e64bc855c4e6445cab63cee753bc1"
  val expectedEncodedAuthHeader = "Basic YTBjZTgwYmMtMTRiNi00OGViLWE4YzAtOTZmMWE5MjdhNTczOjVhOGU2NGJjODU1YzRlNjQ0NWNhYjYzY2VlNzUzYmMx"
  given hc: HeaderCarrier = HeaderCarrier()
  val config = AppConfig(
    Configuration(
      "appName"                            -> "crdl-cache",
      "microservice.services.dps-api.host" -> "localhost",
      "microservice.services.dps-api.path" -> "iv_crdl_reference_data",
      "microservice.services.dps-api.port" -> wireMockPort,
      "microservice.services.dps-api.clientId" -> clientId,
      "microservice.services.dps-api.clientSecret" -> clientSecret,
    )
  )
  val connector =
    new DpsConnector(
      httpClientV2,
      config
    )

  val expectedResponse = CodeListResponse(
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
    )
  )
  override lazy val wireMockRootDirectory = "it/test/resources"
  val uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

  "DpsConnector" should "return a codelist response when DPS API returns 200" in {
    stubFor(
      get(urlPathEqualTo("/iv_crdl_reference_data"))
        .withHeader("correlationId",matching(uuidRegex))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(expectedEncodedAuthHeader))
        .withQueryParam("code_list_code", equalTo("BC08"))
        .willReturn(
          ok().withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON).withBodyFile("BC08.json")
        )
    )
    connector.fetchCodelist(BC08).map { result =>
      result.value mustBe expectedResponse
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

}
