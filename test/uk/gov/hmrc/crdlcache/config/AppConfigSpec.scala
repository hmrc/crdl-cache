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
import uk.gov.hmrc.crdlcache.models.CodeListCode.*
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
        "last-updated-date.default"                          -> "2025-05-29",
        "import-codelists.schedule"                          -> "*/10 * * * * ?",
        "import-correspondence-lists.schedule"               -> "*/10 * * * * ?",
        "import-offices.schedule"                            -> "*/10 * * * * ?",
        "import-codelists.codelists" -> List(
          Map("code" -> "BC08", "origin"  -> "SEED", "keyProperty"  -> "CountryCode"),
          Map("code" -> "BC36", "origin"  -> "SEED", "keyProperty"  -> "ExciseProductCode"),
          Map("code" -> "CL218", "origin" -> "CSRD2", "keyProperty" -> "TransportModeCode")
        ),
        "import-correspondence-lists.correspondence-lists" -> List(
          Map(
            "code"          -> "E200",
            "origin"        -> "SEED",
            "keyProperty"   -> "CnCode",
            "valueProperty" -> "ExciseProductCode"
          )
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
    appConfig.importCorrespondenceListsSchedule mustBe "*/10 * * * * ?"
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 5, 29)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode"),
      CodeListConfig(Unknown("CL218"), CSRD2, "TransportModeCode")
    )
    appConfig.correspondenceListConfigs mustBe List(
      CorrespondenceListConfig(E200, SEED, "CnCode", "ExciseProductCode")
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

    appConfig.importOfficesSchedule mustBe "0 30 4 * * ?"
    appConfig.importCodeListsSchedule mustBe "0 30 23 ? * Tue"
    appConfig.importCorrespondenceListsSchedule mustBe "0 30 23 ? * Tue"
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 11, 3)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC01, SEED, "EvidenceTypeCode"),
      CodeListConfig(BC03, SEED, "AcoActionNotPossibleReasonCode"),
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC09, SEED, "RefusalReasonCode"),
      CodeListConfig(BC11, SEED, "NationalAdministrationCode"),
      CodeListConfig(BC12, SEED, "LanguageCode"),
      CodeListConfig(BC15, SEED, "EventTypeCode"),
      CodeListConfig(BC17, SEED, "KindOfPackages"),
      CodeListConfig(BC22, SEED, "AlertOrRejectionOfMovementReasonCode"),
      CodeListConfig(BC26, SEED, "ReasonForInterruptionCode"),
      CodeListConfig(BC34, SEED, "SubmittingPersonCode"),
      CodeListConfig(BC35, SEED, "TransportUnitCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode"),
      CodeListConfig(BC37, SEED, "CnCode"),
      CodeListConfig(BC40, SEED, "WineGrowingZoneCode"),
      CodeListConfig(BC41, SEED, "WineOperationCode"),
      CodeListConfig(BC43, SEED, "CancellationReasonCode"),
      CodeListConfig(BC46, SEED, "UnsatisfactoryReasonCode"),
      CodeListConfig(BC51, SEED, "DelayExplanationCode"),
      CodeListConfig(BC52, SEED, "UnitOfMeasureCode"),
      CodeListConfig(BC57, SEED, "AcoActionCode"),
      CodeListConfig(BC58, SEED, "DelayedResultReasonCode"),
      CodeListConfig(BC66, SEED, "ExciseProductsCategoryCode"),
      CodeListConfig(BC67, SEED, "TransportModeCode"),
      CodeListConfig(BC106, SEED, "DocumentType"),
      CodeListConfig(BC107, SEED, "ManualClosureRequestReasonCode"),
      CodeListConfig(BC108, SEED, "ManualClosureRejectionReasonCode"),
      CodeListConfig(BC109, SEED, "NationalAdministrationDegreePlatoCode"),
      CodeListConfig(CL239, CSRD2, "AdditionalInformationCode"),
      CodeListConfig(CL380, CSRD2, "DocumentType")
    )
    appConfig.correspondenceListConfigs mustBe List(
      CorrespondenceListConfig(E200, SEED, "CnCode", "ExciseProductCode")
    )
  }
}
