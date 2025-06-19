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

package uk.gov.hmrc.crdlcache.models.dps.col

import play.api.libs.json.{Json, Reads}

case class DpsCustomsOfficeDetail(
  customsofficeusualname: String,
  languagecode: String,
  city: String,
  prefixsuffixflag: String,
  prefixsuffixlevel: Option[String],
  spacetoadd: String,
  streetandnumber: String
)

object DpsCustomsOfficeDetail {
  given Reads[DpsCustomsOfficeDetail] = Json.reads[DpsCustomsOfficeDetail]
}
