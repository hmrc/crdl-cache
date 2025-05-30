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

import play.api.libs.functional.syntax.*
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.*
import play.api.libs.json.OptionHandlers.WritesNull
import uk.gov.hmrc.crdlcache.config.CodeListConfig
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.{CSRD2, SEED}
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  LanguageDescriptionMissing,
  RequiredDataItemMissing,
  RequiredDataItemsMissing
}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

case class CodeListEntry(
  codeListCode: CodeListCode,
  key: String,
  value: String,
  activeFrom: Instant,
  activeTo: Option[Instant],
  updatedAt: Option[Instant],
  properties: JsObject
)

object CodeListEntry {
  private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

  private val knownProperties = Set(
    SEED.activeDateProperty,
    SEED.modificationDateProperty.get,
    CSRD2.activeDateProperty
  )

  private def parseDate(value: String) =
    LocalDate.parse(value, dateFormat).atStartOfDay(ZoneOffset.UTC).toInstant

  def fromDpsEntry(
    config: CodeListConfig,
    dpsEntry: dps.CodeListEntry
  ): CodeListEntry = {
    val key = dpsEntry
      .getProperty(config.keyProperty)
      .flatMap(_.dataitem_value)
      .getOrElse(throw RequiredDataItemMissing(config.keyProperty))

    val value = dpsEntry.language
      .find(_.lang_code.equalsIgnoreCase("en"))
      .map(_.lang_desc)
      .getOrElse(throw LanguageDescriptionMissing)

    val activeFrom = dpsEntry
      .getProperty(config.origin.activeDateProperty)
      .flatMap(_.dataitem_value)
      .map(parseDate)
      .getOrElse(throw RequiredDataItemsMissing(CodeListOrigin.values.map(_.activeDateProperty)*))

    val updatedAt = config.origin.modificationDateProperty
      .flatMap(dpsEntry.getProperty)
      .flatMap(_.dataitem_value)
      .map(parseDate)

    val usedProperties = knownProperties.incl(config.keyProperty)

    val builder = Json.newBuilder

    dpsEntry.dataitem
      .filterNot(item => usedProperties.contains(item.dataitem_name))
      .foreach { item =>
        val propertyValue: JsValueWrapper =
          if (item.propertyName.endsWith("Flag")) item.dataitem_value.contains("1")
          else item.dataitem_value.orNull
        builder += (item.propertyName -> propertyValue)
      }

    CodeListEntry(config.code, key, value, activeFrom, None, updatedAt, builder.result())
  }

  // Only serialize the key, value and properties in JSON responses
  given Writes[CodeListEntry] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "value").write[String] and
      (JsPath \ "properties").write[JsObject]
  )(entry => (entry.key, entry.value, entry.properties))

  // Serialize the full object in MongoDB
  val mongoFormat: Format[CodeListEntry] = {
    // Use the Mongo Extended JSON format for dates
    import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*
    Json.format[CodeListEntry]
  }
}
