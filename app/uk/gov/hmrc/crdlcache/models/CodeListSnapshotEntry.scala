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

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.crdlcache.config.{
  CodeListConfig,
  CorrespondenceListConfig,
  ListConfig,
  PhaseAndDomainListConfig
}
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.{CSRD2, SEED}
import uk.gov.hmrc.crdlcache.models.dps.codelist.DpsCodeListEntry
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  LanguageDescriptionMissing,
  RequiredDataItemMissing,
  RequiredDataItemsMissing,
  UnknownOperation
}
import uk.gov.hmrc.crdlcache.utils.ParserUtils.parseDateToInstant

import java.time.format.DateTimeFormatter
import java.time.Instant

case class CodeListSnapshotEntry(
  key: String,
  value: String,
  activeFrom: Instant,
  updatedAt: Option[Instant],
  operation: Option[Operation],
  properties: JsObject,
  phase: Option[String],
  domain: Option[String]
)

object CodeListSnapshotEntry {
  private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy[ HH:mm:ss]")

  private val knownProperties = Set(
    SEED.activeDateProperty,
    SEED.modificationDateProperty.get,
    SEED.operationProperty.get,
    CSRD2.activeDateProperty
  )

  def fromDpsEntry(
    config: ListConfig,
    dpsEntry: DpsCodeListEntry
  ): CodeListSnapshotEntry = {
    val key = dpsEntry
      .getProperty(config.keyProperty)
      .flatMap(_.dataitem_value)
      .getOrElse(throw RequiredDataItemMissing(config.keyProperty))

    val value = config match {
      case _: CodeListConfig | _: PhaseAndDomainListConfig =>
        dpsEntry.language
          .find(_.lang_code.equalsIgnoreCase("en"))
          .map(_.lang_desc)
          .getOrElse(throw LanguageDescriptionMissing)
      case correspondence: CorrespondenceListConfig =>
        dpsEntry
          .getProperty(correspondence.valueProperty)
          .flatMap(_.dataitem_value)
          .getOrElse(throw RequiredDataItemMissing(correspondence.valueProperty))
    }

    val activeFrom = dpsEntry
      .getProperty(config.origin.activeDateProperty)
      .flatMap(_.dataitem_value)
      .map(parseDateToInstant(_, dateFormat))
      .getOrElse(throw RequiredDataItemsMissing(CodeListOrigin.values.map(_.activeDateProperty)*))

    val updatedAt = config.origin.modificationDateProperty
      .flatMap(dpsEntry.getProperty)
      .flatMap(_.dataitem_value)
      .map(parseDateToInstant(_, dateFormat))

    val operation = config.origin.operationProperty
      .flatMap(dpsEntry.getProperty)
      .flatMap(_.dataitem_value)
      .map { value => Operation.fromString(value).getOrElse(throw UnknownOperation(value)) }

    val usedProperties =
      config match {
        case _: CodeListConfig =>
          knownProperties.incl(config.keyProperty)
        case correspondence: CorrespondenceListConfig =>
          knownProperties.incl(config.keyProperty).incl(correspondence.valueProperty)
        case phaseAndDomain: PhaseAndDomainListConfig =>
          knownProperties.incl(config.keyProperty)
      }

    val builder = Json.newBuilder

    dpsEntry.dataitem
      .filterNot(item => usedProperties.contains(item.dataitem_name))
      .filterNot(item => item.dataitem_value.isEmpty)
      .foreach { item =>
        val propertyValue: JsValueWrapper =
          if (item.propertyName.endsWith("Flag")) item.dataitem_value.contains("1")
          else item.dataitem_value.orNull
        builder += (item.propertyName -> propertyValue)
      }

    val phase: Option[String] = None

    val domain: Option[String] = None

    CodeListSnapshotEntry(
      key,
      value,
      activeFrom,
      updatedAt,
      operation,
      builder.result(),
      phase,
      domain
    )
  }
}
