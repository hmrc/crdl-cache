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

case class DpsCustomsOffice(
  rdentrystatus: RDEntryStatus,
  referencenumber: String,
  referencenumbermainoffice: Option[String],
  referencenumberhigherauthority: Option[String],
  referencenumbercompetentauthorityofenquiry: Option[String],
  referencenumbercompetentauthorityofrecovery: Option[String],
  referencenumbertakeover: Option[String],
  countrycode: String,
  emailaddress: Option[String],
  unlocodeid: Option[String],
  nctsentrydate: Option[String],
  nearestoffice: Option[String],
  postalcode: String,
  phonenumber: Option[String],
  faxnumber: Option[String],
  telexnumber: Option[String],
  geoinfocode: Option[String],
  regioncode: Option[String],
  traderdedicated: String,
  dedicatedtraderlanguagecode: Option[String],
  dedicatedtradername: Option[String],
  customsofficespecificnotes: List[SpecificNotes],
  customsofficelsd: List[DpsCustomsOfficeDetail],
  customsofficetimetable: DpsCustomsOfficeTimetable
)

object DpsCustomsOffice {
  given Reads[DpsCustomsOffice] = Json.reads[DpsCustomsOffice]
}
