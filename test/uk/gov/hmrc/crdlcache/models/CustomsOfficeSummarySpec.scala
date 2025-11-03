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

import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class CustomsOfficeSummarySpec extends AnyFlatSpec with Matchers {
  val expectedReferenceNumber = "A1B2C3B4E5"
  val expectedCountryCode     = "CC"
  val expectedUsualName       = "Test Usual Name"

  val document: Document = Document(
    Json
      .obj(
        "referenceNumber" -> expectedReferenceNumber,
        "countryCode"     -> expectedCountryCode,
        "customsOfficeLsd" -> Json.obj(
          "customsOfficeUsualName" -> expectedUsualName
        )
      )
      .toString
  )

  "getReferenceNumber" should "return the expected value from the Document" in {
    CustomsOfficeSummary.getReferenceNumber(document) mustBe expectedReferenceNumber
  }

  "getCountryCode" should "return the expected value from the Document" in {
    CustomsOfficeSummary.getCountryCode(document) mustBe expectedCountryCode
  }

  "getUsualName" should "return the expected value from the Document" in {
    CustomsOfficeSummary.getUsualName(document) mustBe expectedUsualName
  }
}
