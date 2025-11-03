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

import play.api.libs.json.{Json, Writes}
import org.mongodb.scala.bson.collection.immutable.Document

final case class CustomsOfficeSummary(
  referenceNumber: String,
  countryCode: String,
  customsOfficeUsualName: String
)

object CustomsOfficeSummary {
  given Writes[CustomsOfficeSummary] = Json.writes[CustomsOfficeSummary]

  private val referenceNumberKey = "referenceNumber"
  private val countryCodeKey     = "countryCode"
  private val officeLsdKey       = "customsOfficeLsd"
  private val usualNameKey       = "customsOfficeUsualName"
  private def getDocumentKey(document: Document, key: String) =
    document.find(doc => doc._1 == key).fold("")(bson => bson._2.asString().getValue())
  def getReferenceNumber(document: Document) = getDocumentKey(document, referenceNumberKey)
  def getCountryCode(document: Document)     = getDocumentKey(document, countryCodeKey)
  def getUsualName(document: Document) = document
    .find(doc => doc._1 == officeLsdKey)
    .fold("")(bson => bson._2.asDocument().getString(usualNameKey).getValue())
}
