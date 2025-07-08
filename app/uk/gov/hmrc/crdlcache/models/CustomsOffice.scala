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

import uk.gov.hmrc.crdlcache.models.CustomsOfficeDetail.fromDpsCustomsOfficeDetail
import play.api.libs.json.*
import uk.gov.hmrc.crdlcache.models.CustomsOfficeTimetable.fromDpsCustomsOfficeTimetable
import uk.gov.hmrc.crdlcache.models.dps.col.DpsCustomsOffice
import uk.gov.hmrc.crdlcache.models.errors.ImportError.CustomsOfficeDetailMissing
import uk.gov.hmrc.crdlcache.utils.ParserUtils.{parseDate, parseDateToInstant}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

case class CustomsOffice(
  referenceNumber: String,
  activeFrom: Instant,
  activeTo: Option[Instant],
  referenceNumberMainOffice: Option[String],
  referenceNumberHigherAuthority: Option[String],
  referenceNumberCompetentAuthorityOfEnquiry: Option[String],
  referenceNumberCompetentAuthorityOfRecovery: Option[String],
  referenceNumberTakeover: Option[String],
  countryCode: String,
  emailAddress: Option[String],
  unLocodeId: Option[String],
  nctsEntryDate: Option[LocalDate],
  nearestOffice: Option[String],
  postalCode: String,
  phoneNumber: Option[String],
  faxNumber: Option[String],
  telexNumber: Option[String],
  geoInfoCode: Option[String],
  regionCode: Option[String],
  traderDedicated: Boolean,
  dedicatedTraderLanguageCode: Option[String],
  dedicatedTraderName: Option[String],
  customsOfficeSpecificNotesCodes: List[String],
  customsOfficeLsd: CustomsOfficeDetail,
  customsOfficeTimetable: CustomsOfficeTimetable
)

object CustomsOffice {
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
    )
  }

  val mongoFormat: Format[CustomsOffice] = { // Use the Mongo Extended JSON format for dates
    import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*
    Json.format[CustomsOffice]
  }

  private val ukLocalDate  = DateTimeFormatter.ofPattern("dd-MM-yyyy")
  private val basicIsoDate = DateTimeFormatter.BASIC_ISO_DATE

  def fromDpsCustomOfficeList(dpsCustomOfficeList: DpsCustomsOffice): CustomsOffice = {
    CustomsOffice(
      dpsCustomOfficeList.referencenumber,
      parseDateToInstant(dpsCustomOfficeList.rdentrystatus.activefrom, ukLocalDate),
      None,
      dpsCustomOfficeList.referencenumbermainoffice,
      dpsCustomOfficeList.referencenumberhigherauthority,
      dpsCustomOfficeList.referencenumbercompetentauthorityofenquiry,
      dpsCustomOfficeList.referencenumbercompetentauthorityofrecovery,
      dpsCustomOfficeList.referencenumbertakeover,
      dpsCustomOfficeList.countrycode,
      dpsCustomOfficeList.emailaddress,
      dpsCustomOfficeList.unlocodeid,
      dpsCustomOfficeList.nctsentrydate.map(parseDate(_, basicIsoDate)),
      dpsCustomOfficeList.nearestoffice,
      dpsCustomOfficeList.postalcode,
      dpsCustomOfficeList.phonenumber,
      dpsCustomOfficeList.faxnumber,
      dpsCustomOfficeList.telexnumber,
      dpsCustomOfficeList.geoinfocode,
      dpsCustomOfficeList.regioncode,
      dpsCustomOfficeList.traderdedicated == "1",
      dpsCustomOfficeList.dedicatedtraderlanguagecode,
      dpsCustomOfficeList.dedicatedtradername,
      dpsCustomOfficeList.customsofficespecificnotes.map(
        _.specificnotescode
      ),
      fromDpsCustomsOfficeDetail(
        dpsCustomOfficeList.customsofficelsd.headOption
          .getOrElse(throw CustomsOfficeDetailMissing(dpsCustomOfficeList.referencenumber))
      ),
      fromDpsCustomsOfficeTimetable(dpsCustomOfficeList.customsofficetimetable, basicIsoDate)
    )

  }
}
