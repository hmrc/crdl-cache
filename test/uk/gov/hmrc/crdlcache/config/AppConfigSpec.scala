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

package uk.gov.hmrc.crdlcache.config

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC36, BC66, CL239, CL380, Unknown}
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.{CSRD2, SEED}

import java.time.LocalDate

class AppConfigSpec extends AnyFlatSpec with Matchers {
  "AppConfig" should "load mandatory app configuration from Configuration" in {
    val appConfig = new AppConfig(
      Configuration(
        "appName"                                            -> "crdl-cache",
        "microservice.services.dps-api.host"                 -> "localhost",
        "microservice.services.dps-api.port"                 -> 7255,
        "microservice.services.dps-api.ref-data-path"        -> "views/iv_crdl_reference_data",
        "microservice.services.dps-api.customs-offices-path" -> "views/iv_crdl_customs_office",
        "microservice.services.dps-api.clientId"             -> "abc123",
        "microservice.services.dps-api.clientSecret"         -> "def456",
        "import-codelists.schedule"                          -> "*/10 * * * * ?",
        "import-offices.schedule"                            -> "*/10 * * * * ?",
        "import-codelists.last-updated-date.default"         -> "2025-05-29",
        "import-codelists.codelists" -> List(
          Map("code" -> "BC08", "origin" -> "SEED", "keyProperty" -> "CountryCode"),
          Map("code" -> "BC36", "origin" -> "SEED", "keyProperty" -> "ExciseProductCode"),
          Map("code" -> "BC17", "origin" -> "SEED", "keyProperty" -> "KindOfPackages")
        )
      )
    )

    appConfig.appName mustBe "crdl-cache"

    appConfig.dpsUrl mustBe "http://localhost:7255"
    appConfig.dpsRefDataPath mustBe "views/iv_crdl_reference_data"
    appConfig.dpsCustomsOfficesPath mustBe "views/iv_crdl_customs_office"
    appConfig.dpsClientId mustBe "abc123"
    appConfig.dpsClientSecret mustBe "def456"

    appConfig.importCodeListsSchedule mustBe "*/10 * * * * ?"
    appConfig.importOfficesSchedule mustBe "*/10 * * * * ?"
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 5, 29)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode"),
      CodeListConfig(Unknown("BC17"), SEED, "KindOfPackages")
    )
  }

  it should "load mandatory app configuration from application.conf" in {
    val appConfig = new AppConfig(new Configuration(ConfigFactory.load()))
    appConfig.appName mustBe "crdl-cache"

    appConfig.dpsUrl mustBe "http://localhost:7253"
    appConfig.dpsRefDataPath mustBe "crdl-ref-data-dps-stub/iv_crdl_reference_data"
    appConfig.dpsCustomsOfficesPath mustBe "crdl-ref-data-dps-stub/iv_crdl_customs_office"
    appConfig.dpsClientId mustBe "client_id_must_be_set_in_app-config-xxx"
    appConfig.dpsClientSecret mustBe "client_secret_must_be_set_in_app-config-xxx"

    appConfig.importOfficesSchedule mustBe "0 0 4 * * ?"
    appConfig.importCodeListsSchedule mustBe "0 0 4 * * ?"
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 3, 12)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode"),
      CodeListConfig(BC66, SEED, "ExciseProductsCategoryCode"),
      CodeListConfig(CL239, CSRD2, "AdditionalInformationCode"),
      CodeListConfig(CL380, CSRD2, "DocumentType")
    )
  }
}
