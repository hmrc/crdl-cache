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

package uk.gov.hmrc.crdlcache.models.formats

import play.api.libs.json.{JsPath, Json, Writes}
import uk.gov.hmrc.crdlcache.models.{
  CodeListCode,
  CodeListEntry,
  CustomsOffice,
  CustomsOfficeDetail,
  CustomsOfficeTimetable,
  LastUpdated,
  RoleTrafficCompetence,
  TimetableLine
}

trait HttpFormats extends JavaTimeFormats {

  given codeListEntryWrites: Writes[CodeListEntry] = Writes { entry =>
    Json.obj(
      "key"        -> entry.key,
      "value"      -> entry.value,
      "properties" -> entry.properties
    ) // Don't serialize activation dates
  }

  given Writes[LastUpdated] = Json.writes[LastUpdated]

  given Writes[CustomsOffice] = Writes { office =>
    Json.obj(
      "referenceNumber"                -> office.referenceNumber,
      "referenceNumberMainOffice"      -> office.referenceNumberMainOffice,
      "referenceNumberHigherAuthority" -> office.referenceNumberHigherAuthority,
      "referenceNumberCompetentAuthorityOfEnquiry" -> office.referenceNumberCompetentAuthorityOfEnquiry,
      "referenceNumberCompetentAuthorityOfRecovery" -> office.referenceNumberCompetentAuthorityOfRecovery,
      "referenceNumberTakeover"         -> office.referenceNumberTakeover,
      "countryCode"                     -> office.countryCode,
      "emailAddress"                    -> office.emailAddress,
      "unLocodeId"                      -> office.unLocodeId,
      "nctsEntryDate"                   -> office.nctsEntryDate,
      "nearestOffice"                   -> office.nearestOffice,
      "postalCode"                      -> office.postalCode,
      "phoneNumber"                     -> office.phoneNumber,
      "faxNumber"                       -> office.faxNumber,
      "telexNumber"                     -> office.telexNumber,
      "geoInfoCode"                     -> office.geoInfoCode,
      "regionCode"                      -> office.regionCode,
      "traderDedicated"                 -> office.traderDedicated,
      "dedicatedTraderLanguageCode"     -> office.dedicatedTraderLanguageCode,
      "dedicatedTraderName"             -> office.dedicatedTraderName,
      "customsOfficeSpecificNotesCodes" -> office.customsOfficeSpecificNotesCodes,
      "customsOfficeLsd"                -> office.customsOfficeLsd,
      "customsOfficeTimetable"          -> office.customsOfficeTimetable
    ) // Don't serialize activation dates
  }

  given Writes[CustomsOfficeDetail] = Json.writes[CustomsOfficeDetail]

  given Writes[CustomsOfficeTimetable] = Json.writes[CustomsOfficeTimetable]

  given Writes[TimetableLine] = Json.writes[TimetableLine]

  given Writes[RoleTrafficCompetence] = Json.writes[RoleTrafficCompetence]

}

object HttpFormats extends HttpFormats
