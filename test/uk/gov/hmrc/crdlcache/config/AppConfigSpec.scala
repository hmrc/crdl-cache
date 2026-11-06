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
import uk.gov.hmrc.crdlcache.models.DomainAndPhase

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
        "last-updated-date.default"                          -> "2025-11-01",
        "customsOfficesPageSize"                             -> "50",
        "import-codelists.schedule"                          -> "*/10 * * * * ?",
        "import-pd-lists.schedule"                           -> "*/10 * * * * ?",
        "import-correspondence-lists.schedule"               -> "*/10 * * * * ?",
        "import-offices.schedule"                            -> "*/10 * * * * ?",
        "import-pd-lists.phase"                              -> "P6",
        "import-pd-lists.domain"                             -> "NCTS",
        "import-offices.phase"                               -> "P6",
        "import-offices.domain"                              -> "NCTS",
        "import-codelists.codelists" -> List(
          Map("code" -> "BC08", "origin" -> "SEED", "keyProperty" -> "CountryCode"),
          Map("code" -> "BC36", "origin" -> "SEED", "keyProperty" -> "ExciseProductCode")
        ),
        "import-pd-lists.pd-lists" -> List(
          Map(
            "code"        -> "CL231",
            "origin"      -> "CSRD2",
            "keyProperty" -> "DeclarationTypeCode"
          ),
          Map(
            "code"        -> "CL234",
            "origin"      -> "CSRD2",
            "keyProperty" -> "DocumentTypeExciseCode"
          )
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
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 11, 1)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode")
    )
    appConfig.domainAndPhaseConfigs mustBe List(
      DomainAndPhaseListConfig(CL231, CSRD2, "DeclarationTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL234, CSRD2, "DocumentTypeExciseCode", "NCTS", "P6")
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
//    appConfig.importPhaseAndDomainListsSchedule mustBe "0 00 06 ? * *"
    appConfig.importCorrespondenceListsSchedule mustBe "0 30 23 ? * Tue"
    appConfig.importOfficesJobDomainAndPhase mustBe DomainAndPhase(Some("NCTS"), Some("P6"))
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 11, 1)
    appConfig.customsOfficesPageSize mustBe 50
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
      CodeListConfig(HMRCBC36, SEED, "ExciseProductCode"),
      CodeListConfig(HMRCBC37, SEED, "CnCode"),
      CodeListConfig(HMRCBC66, SEED, "ExciseProductsCategoryCode")
    )
    appConfig.domainAndPhaseConfigs mustBe List(
      DomainAndPhaseListConfig(CL008, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL009, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL010, CSRD2, "CountryCode", "NCTS", "P6"),
      // PhaseAndDomainListConfig(CL016, CSRD2, "CUSCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL017, CSRD2, "KindOfPackages", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL019, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL030, CSRD2, "XmlErrorCodesCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL038, CSRD2, "QualifierOfTheIdentification", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL042, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL048, CSRD2, "Currency", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL056, CSRD2, "Role", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL076, CSRD2, "GuaranteeTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL094, CSRD2, "RepresentativeStatusCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL101, CSRD2, "UnDangerousGoodsCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL112, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL116, CSRD2, "TransportChargesMethodOfPayment", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL147, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL152, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL165, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL167, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL178, CSRD2, "PreviousDocumentTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL180, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL181, CSRD2, "KindOfPackages", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL182, CSRD2, "KindOfPackages", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL190, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL198, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL213, CSRD2, "SupportingDocumentCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL214, CSRD2, "PreviousDocumentTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL215, CSRD2, "DocumentType", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL217, CSRD2, "DeclarationTypeSecurityCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL218, CSRD2, "TransportModeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(
        CL219,
        CSRD2,
        "TypeOfIdentificationofMeansOfTransportActiveCode",
        "NCTS",
        "P6"
      ),
      DomainAndPhaseListConfig(CL226, CSRD2, "RejectionDepartureExportCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL228, CSRD2, "PreviousDocumentTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL229, CSRD2, "GuaranteeTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL230, CSRD2, "GuaranteeTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL231, CSRD2, "DeclarationTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL232, CSRD2, "DeclarationTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL234, CSRD2, "PreviousDocumentTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL235, CSRD2, "AuthorisationType", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL236, CSRD2, "AuthorisationType", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL239, CSRD2, "AdditionalInformationCode", "NCTS", "P6"),
      // PhaseAndDomainListConfig(CL244, CSRD2, "UnLocodeExtendedCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL248, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL251, CSRD2, "GuaranteeTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL252, CSRD2, "InvalidGuaranteeReasonCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL286, CSRD2, "GuaranteeTypeCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL296, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL326, CSRD2, "QualifierOfTheIdentification", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL347, CSRD2, "TypeOfLocation", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL349, CSRD2, "Unit", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL380, CSRD2, "DocumentType", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL437, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL505, CSRD2, "CountryCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL560, CSRD2, "BusinessRejectionTypeDepExpCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL580, CSRD2, "BusinessRejectionTypeTraCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL581, CSRD2, "RejectionCodeTransitCode", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL704, CSRD2, "Role", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL716, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL750, CSRD2, "TypeOfIdentification", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL752, CSRD2, "Code", "NCTS", "P6"),
      DomainAndPhaseListConfig(CL754, CSRD2, "Type", "NCTS", "P6")
    )
    appConfig.correspondenceListConfigs mustBe List(
      CorrespondenceListConfig(E200, SEED, "CnCode", "ExciseProductCode"),
      CorrespondenceListConfig(HMRCE200, SEED, "CnCode", "ExciseProductCode")
    )
  }
}
