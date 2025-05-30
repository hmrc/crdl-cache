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
import uk.gov.hmrc.crdlcache.config.CodeListConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.SEED
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  LanguageDescriptionMissing,
  RequiredDataItemMissing,
  RequiredDataItemsMissing
}

import java.time.Instant

class CodeListEntrySpec extends AnyFlatSpec with Matchers with TestData {
  private val BC08Config = CodeListConfig(BC08, SEED, "CountryCode")

  val convertedEntry = CodeListEntry(
    BC08,
    "AW",
    "Aruba",
    Instant.parse("2024-01-17T00:00:00Z"),
    None,
    Some(Instant.parse("2024-01-17T00:00:00Z")),
    Json.obj(
      "operation"              -> "U",
      "actionIdentification"   -> "811",
      "responsibleDataManager" -> (null: String)
    )
  )

  val mongoJson = Json.obj(
    "codeListCode" -> "BC08",
    "key"          -> "AW",
    "value"        -> "Aruba",
    "activeFrom" -> Json.obj(
      "$date" -> Json.obj("$numberLong" -> convertedEntry.activeFrom.toEpochMilli.toString)
    ),
    "updatedAt" -> Json.obj(
      "$date" -> Json.obj("$numberLong" -> convertedEntry.updatedAt.get.toEpochMilli.toString)
    ),
    "properties" -> Json.obj(
      "operation"              -> "U",
      "actionIdentification"   -> "811",
      "responsibleDataManager" -> (null: String)
    )
  )

  "CodeListEntry.fromDpsEntry" should "convert a sample BC08 codelist entry" in {
    CodeListEntry.fromDpsEntry(BC08Config, BC08Aruba) mustBe convertedEntry
  }

  it should "fail when the key property is missing" in {
    val dataItemMissing = the[RequiredDataItemMissing] thrownBy CodeListEntry.fromDpsEntry(
      BC08Config,
      dps.CodeListEntry(List.empty, List.empty)
    )

    dataItemMissing.itemName mustBe BC08Config.keyProperty
  }

  it should "fail when the language description is missing" in {
    assertThrows[LanguageDescriptionMissing.type] {
      CodeListEntry.fromDpsEntry(
        BC08Config,
        dps.CodeListEntry(List(dps.DataItem(BC08Config.keyProperty, Some("AW"))), List.empty)
      )
    }
  }

  it should "fail when the activation date property is missing" in {
    val dataItemsMissing = the[RequiredDataItemsMissing] thrownBy CodeListEntry.fromDpsEntry(
      BC08Config,
      dps.CodeListEntry(
        List(dps.DataItem(BC08Config.keyProperty, Some("AW"))),
        List(dps.LanguageDescription("en", "Aruba"))
      )
    )

    dataItemsMissing.itemNames mustBe Seq("Action_ActivationDate", "RDEntry_activeFrom")
  }

  it should "succeed when the modification date property is missing" in {
    val inputEntry = dps.CodeListEntry(
      List(
        dps.DataItem(BC08Config.keyProperty, Some("AW")),
        dps.DataItem("Action_ActivationDate", Some("17-01-2024"))
      ),
      List(dps.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj()
    )

    CodeListEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "convert '*Flag' properties that equal String '1' to Boolean 'true'" in {
    val inputEntry = dps.CodeListEntry(
      List(
        dps.DataItem(BC08Config.keyProperty, Some("AW")),
        dps.DataItem("Action_ActivationDate", Some("17-01-2024")),
        dps.DataItem("DensityApplicabilityFlag", Some("1"))
      ),
      List(dps.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj("densityApplicabilityFlag" -> true)
    )

    CodeListEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  it should "convert '*Flag' properties that equal anything else to Boolean 'false'" in {
    val inputEntry = dps.CodeListEntry(
      List(
        dps.DataItem(BC08Config.keyProperty, Some("AW")),
        dps.DataItem("Action_ActivationDate", Some("17-01-2024")),
        dps.DataItem("DensityApplicabilityFlag", Some("0"))
      ),
      List(dps.LanguageDescription("en", "Aruba"))
    )

    val expectedEntry = CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      None,
      Json.obj("densityApplicabilityFlag" -> false)
    )

    CodeListEntry.fromDpsEntry(BC08Config, inputEntry) mustBe expectedEntry
  }

  "The default Writes for CodeListEntry" should "serialize only the key, value and properties" in {
    Json.toJson(convertedEntry) mustBe Json.obj(
      "key"   -> "AW",
      "value" -> "Aruba",
      "properties" -> Json.obj(
        "operation"              -> "U",
        "actionIdentification"   -> "811",
        "responsibleDataManager" -> (null: String)
      )
    )
  }

  "The MongoDB format for CodeListEntry" should "serialize all properties as Mongo Extended JSON" in {
    Json.toJson(convertedEntry)(CodeListEntry.mongoFormat) mustBe mongoJson
  }

  it should "deserialize all properties from Mongo Extended JSON" in {
    Json.fromJson[CodeListEntry](mongoJson)(CodeListEntry.mongoFormat).get mustBe convertedEntry
  }
}
