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
        "import-pd-lists.schedule"                           -> "*/10 * * * * ?",
        "import-correspondence-lists.schedule"               -> "*/10 * * * * ?",
        "import-offices.schedule"                            -> "*/10 * * * * ?",
        "import-pd-lists.phase"                              -> "P6",
        "import-pd-lists.domain"                             -> "NCTS",
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
    appConfig.defaultLastUpdated mustBe LocalDate.of(2025, 5, 29)
    appConfig.codeListConfigs mustBe List(
      CodeListConfig(BC08, SEED, "CountryCode"),
      CodeListConfig(BC36, SEED, "ExciseProductCode")
    )
    appConfig.phaseAndDomainListConfigs mustBe List(
      PhaseAndDomainListConfig(CL231, CSRD2, "DeclarationTypeCode", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL234, CSRD2, "DocumentTypeExciseCode", Some("P6"), Some("NCTS"))
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
    appConfig.importPhaseAndDomainListsSchedule mustBe "0 30 23 ? * Tue"
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
      CodeListConfig(BC109, SEED, "NationalAdministrationDegreePlatoCode")
    )
    appConfig.phaseAndDomainListConfigs mustBe List(
      PhaseAndDomainListConfig(
        CL008,
        CSRD2,
        "CountryCodesFullList",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL009,
        CSRD2,
        "CountryCodesCommonTransit",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL010,
        CSRD2,
        "CountryCodesCommunity",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL017, CSRD2, "KindOfPackages", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL019, CSRD2, "IncidentCode", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL030, CSRD2, "XmlErrorCodes", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL038,
        CSRD2,
        "QualifierOfIdentificationIncident",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL042,
        CSRD2,
        "DeclarationTypeAdditional",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL048, CSRD2, "CurrencyCodes", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL056, CSRD2, "Role", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL076,
        CSRD2,
        "GuaranteeTypeWithReference",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL094,
        CSRD2,
        "RepresentativeStatusCode",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL112,
        CSRD2,
        "CountryCodesCTC",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL116,
        CSRD2,
        "TransportChargesMethodOfPayment",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL147,
        CSRD2,
        "CountryCustomsSecurityAgreementArea",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL165, CSRD2, "Nationality", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL167,
        CSRD2,
        "CountryCodesOptout",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL178,
        CSRD2,
        "PreviousDocumentUnionGoods",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL180,
        CSRD2,
        "FunctionalErrorCodesIeCA",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL181,
        CSRD2,
        "KindOfPackagesBulk",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL182,
        CSRD2,
        "KindOfPackagesUnpacked",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL190,
        CSRD2,
        "CountryAddressPostcodeBased",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL198,
        CSRD2,
        "CountryAddressPostcodeOnly",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL213,
        CSRD2,
        "SupportingDocumentType",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL214,
        CSRD2,
        "PreviousDocumentType",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL215,
        CSRD2,
        "RequestedDocumentType",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL217,
        CSRD2,
        "DeclarationTypeSecurity",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL218,
        CSRD2,
        "TransportModeCode",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL219,
        CSRD2,
        "TypeOfIdentificationofMeansOfTransportActive",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL226,
        CSRD2,
        "RejectionCodeDepartureExport",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL228,
        CSRD2,
        "PreviousDocumentExportType",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL229,
        CSRD2,
        "GuaranteeTypeCTC",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL230,
        CSRD2,
        "GuaranteeTypeEUNonTIR",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL231, CSRD2, "DeclarationTypeCode", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL232,
        CSRD2,
        "DeclarationTypeItemLevel",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL234, CSRD2, "DocumentTypeExciseCode", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL235,
        CSRD2,
        "AuthorisationTypeDeparture",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL236,
        CSRD2,
        "AuthorisationTypeDestination",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL239, CSRD2, "AdditionalInformation", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL248,
        CSRD2,
        "CountryCodesForAddress",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL251, CSRD2, "GuaranteeType", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL252,
        CSRD2,
        "InvalidGuaranteeReason",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL286,
        CSRD2,
        "GuaranteeTypeWithGRN",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL296,
        CSRD2,
        "SpecificCircumstanceIndicatorCode",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL326,
        CSRD2,
        "QualifierOfTheIdentification",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL347, CSRD2, "TypeOfLocation", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL349, CSRD2, "Unit", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(CL380, CSRD2, "AdditionalReference", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL437,
        CSRD2,
        "FunctionErrorCodesTED",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL560,
        CSRD2,
        "BusinessRejectionTypeDepExp",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL580,
        CSRD2,
        "BusinessRejectionTypeTra",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL581,
        CSRD2,
        "RejectionCodeTransit",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL704,
        CSRD2,
        "AdditionalSupplyChainActorRoleCode",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(CL716, CSRD2, "ControlType", Some("P6"), Some("NCTS")),
      PhaseAndDomainListConfig(
        CL750,
        CSRD2,
        "TypeOfIdentificationOfMeansOfTransport",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL752,
        CSRD2,
        "AdditionalInformationCodeSubset",
        Some("P6"),
        Some("NCTS")
      ),
      PhaseAndDomainListConfig(
        CL754,
        CSRD2,
        "TransportDocumentType",
        Some("P6"),
        Some("NCTS")
      )
    )
    appConfig.correspondenceListConfigs mustBe List(
      CorrespondenceListConfig(E200, SEED, "CnCode", "ExciseProductCode")
    )
  }
}
