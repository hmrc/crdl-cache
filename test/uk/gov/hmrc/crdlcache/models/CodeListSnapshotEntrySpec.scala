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

package uk.gov.hmrc.crdlcache.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.config.{CodeListConfig, CorrespondenceListConfig}
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, E200}
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.SEED
import uk.gov.hmrc.crdlcache.models.Operation.{Create, Update}
import uk.gov.hmrc.crdlcache.models.dps.codelist
import uk.gov.hmrc.crdlcache.models.dps.codelist.{DpsCodeListEntry, DataItem, LanguageDescription}
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  LanguageDescriptionMissing,
  RequiredDataItemMissing,
  RequiredDataItemsMissing,
  UnknownOperation
}

import java.time.Instant

class CodeListSnapshotEntrySpec extends AnyFlatSpec with Matchers with TestData {
  private val BC08Config = CodeListConfig(BC08, SEED, "CountryCode")
  private val E200Config = CorrespondenceListConfig(E200, SEED, "CnCode", "ExciseProductCode")

  private val convertedEntry = CodeListSnapshotEntry(
    "AW",
    "Aruba",
    Instant.parse("2024-01-17T00:00:00Z"),
    Some(Instant.parse("2024-01-17T00:00:00Z")),
    Some(Update),
    Json.obj(
      "actionIdentification" -> "811"
    )
  )

  "CodeListSnapshotEntry.fromDpsEntry" should "convert a sample BC08 codelist entry" in {
    CodeListSnapshotEntry.fromDpsEntry(BC08Config, BC08Aruba) mustBe convertedEntry
  }

  it should "fail when the key property is missing" in {
    val dataItemMissing = the[RequiredDataItemMissing] thrownBy CodeListSnapshotEntry.fromDpsEntry(
      BC08Config,
      DpsCodeListEntry(List.empty, List.empty)
    )

    dataItemMissing.itemName mustBe BC08Config.keyProperty
  }

  it should "fail when the language description is missing" in {
    assertThrows[LanguageDescriptionMissing.type] {
      CodeListSnapshotEntry.fromDpsEntry(
        BC08Config,
        DpsCodeListEntry(List(DataItem(BC08Config.keyProperty, Some("AW"))), List.empty)
      )
    }
  }

  it should "fail when importing a correspondence list if the value property is missing" in {
    assertThrows[RequiredDataItemMissing] {
      CodeListSnapshotEntry.fromDpsEntry(
        E200Config,
        DpsCodeListEntry(List(DataItem(E200Config.keyProperty, Some("27101944"))), List.empty)
      )
    }
  }

  it should "fail when the activation date property is missing" in {
    val dataItemsMissing =
      the[RequiredDataItemsMissing] thrownBy CodeListSnapshotEntry.fromDpsEntry(
        BC08Config,
        DpsCodeListEntry(
          List(codelist.DataItem(BC08Config.keyProperty, Some("AW"))),
          List(LanguageDescription("en", "Aruba"))
        )
      )

    dataItemsMissing.itemNames mustBe Seq("Action_ActivationDate", "RDEntryStatus_activeFrom")
  }

  it should "succeed when the operation property is missing" in {
    val inputEntry = DpsCodeListEntry(
      List(
        codelist.DataItem(BC08Config.keyProperty, Some("AW")),
        codelist.DataItem("Action_ActivationDate", Some("17-01-2024"))
      ),
      List(codelist.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListSnapshotEntry(
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj()
    )

    CodeListSnapshotEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "succeed when the modification date property is missing" in {
    val inputEntry = DpsCodeListEntry(
      List(
        codelist.DataItem(BC08Config.keyProperty, Some("AW")),
        codelist.DataItem("Action_ActivationDate", Some("17-01-2024"))
      ),
      List(codelist.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListSnapshotEntry(
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj()
    )

    CodeListSnapshotEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "fail when the operation property does not correspond to any known operation" in {
    val unknownOperation =
      the[UnknownOperation] thrownBy CodeListSnapshotEntry.fromDpsEntry(
        BC08Config,
        DpsCodeListEntry(
          List(
            codelist.DataItem(BC08Config.keyProperty, Some("AW")),
            codelist.DataItem("Action_ActivationDate", Some("17-01-2024")),
            codelist.DataItem("Action_Operation", Some("X"))
          ),
          List(codelist.LanguageDescription("en", "Aruba"))
        )
      )

    unknownOperation.code mustBe "X"
  }

  it should "succeed when the operation property is recognised" in {
    val inputEntry = DpsCodeListEntry(
      List(
        codelist.DataItem(BC08Config.keyProperty, Some("AW")),
        codelist.DataItem("Action_ActivationDate", Some("17-01-2024")),
        codelist.DataItem("Action_Operation", Some("C"))
      ),
      List(codelist.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListSnapshotEntry(
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Create),
      Json.obj()
    )

    CodeListSnapshotEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "convert '*Flag' properties that equal String '1' to Boolean 'true'" in {
    val inputEntry = DpsCodeListEntry(
      List(
        codelist.DataItem(BC08Config.keyProperty, Some("AW")),
        codelist.DataItem("Action_ActivationDate", Some("17-01-2024")),
        codelist.DataItem("DensityApplicabilityFlag", Some("1"))
      ),
      List(codelist.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListSnapshotEntry(
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj("densityApplicabilityFlag" -> true)
    )

    CodeListSnapshotEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "convert '*Flag' properties that equal anything else to Boolean 'false'" in {
    val inputEntry = DpsCodeListEntry(
      List(
        codelist.DataItem(BC08Config.keyProperty, Some("AW")),
        codelist.DataItem("Action_ActivationDate", Some("17-01-2024")),
        codelist.DataItem("DensityApplicabilityFlag", Some("0"))
      ),
      List(codelist.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListSnapshotEntry(
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj("densityApplicabilityFlag" -> false)
    )

    CodeListSnapshotEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }
}
