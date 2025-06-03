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
import uk.gov.hmrc.crdlcache.models.CodeListCode.BC08

import java.time.Instant

class CodeListEntrySpec extends AnyFlatSpec with Matchers with TestData {
  private val sampleEntry = CodeListEntry(
    BC08,
    "AW",
    "Aruba",
    Instant.parse("2024-01-17T00:00:00Z"),
    None,
    Some(Instant.parse("2024-01-17T00:00:00Z")),
    Json.obj(
      "actionIdentification"   -> "811",
      "responsibleDataManager" -> (null: String)
    )
  )

  private val mongoJson = Json.obj(
    "codeListCode" -> "BC08",
    "key"          -> "AW",
    "value"        -> "Aruba",
    "activeFrom" -> Json.obj(
      "$date" -> Json.obj("$numberLong" -> sampleEntry.activeFrom.toEpochMilli.toString)
    ),
    "updatedAt" -> Json.obj(
      "$date" -> Json.obj("$numberLong" -> sampleEntry.updatedAt.get.toEpochMilli.toString)
    ),
    "properties" -> Json.obj(
      "actionIdentification"   -> "811",
      "responsibleDataManager" -> (null: String)
    )
  )

  "The default Writes for CodeListEntry" should "serialize only the key, value and properties" in {
    Json.toJson(sampleEntry) mustBe Json.obj(
      "key"   -> "AW",
      "value" -> "Aruba",
      "properties" -> Json.obj(
        "actionIdentification"   -> "811",
        "responsibleDataManager" -> (null: String)
      )
    )
  }

  "The MongoDB format for CodeListEntry" should "serialize all properties as Mongo Extended JSON" in {
    Json.toJson(sampleEntry)(CodeListEntry.mongoFormat) mustBe mongoJson
  }

  it should "deserialize all properties from Mongo Extended JSON" in {
    Json.fromJson[CodeListEntry](mongoJson)(CodeListEntry.mongoFormat).get mustBe sampleEntry
  }
}
