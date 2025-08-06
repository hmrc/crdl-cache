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
import uk.gov.hmrc.crdlcache.models.dps.col.{DpsCustomsOfficeDetail, RDEntryStatus}
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  CustomsOfficeDetailMissing,
  InvalidDateFormat
}

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, LocalDate, LocalTime}

class CustomsOfficeSpec extends AnyFlatSpec with Matchers with TestData {
  val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
  val inputOffice = DK003102.copy(customsofficelsd =
    List(
      DpsCustomsOfficeDetail(
        "Hirtshals Toldekspedition",
        "DA",
        "Hirtshals",
        "0",
        None,
        None,
        "0",
        "Dalsagervej 7"
      ),
      DpsCustomsOfficeDetail(
        "Hirtshals",
        "en",
        "Hirtshals",
        "0",
        None,
        None,
        "1",
        "Test 7"
      )
    )
  )
  val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  val expectedSnapshot = CustomsOffice(
    "DK003102",
    Instant.parse("2025-03-22T00:00:00Z"),
    None,
    None,
    None,
    Some("DK003102"),
    Some("DK003102"),
    None,
    "DK",
    Some("test@dk"),
    None,
    None,
    None,
    "9850",
    Some("+45 342234 34543"),
    None,
    None,
    None,
    None,
    false,
    None,
    None,
    List("SN0009"),
    CustomsOfficeDetail(
      "Hirtshals",
      "en",
      "Hirtshals",
      false,
      None,
      None,
      true,
      "Test 7"
    ),
    List(
      CustomsOfficeTimetable(
        1,
        None,
        LocalDate.parse("20180101", dateFormat),
        LocalDate.parse("20991231", dateFormat),
        List(
          TimetableLine(
            DayOfWeek.of(1),
            LocalTime.parse("0800", timeFormat),
            LocalTime.parse("1600", timeFormat),
            DayOfWeek.of(5),
            None,
            None,
            List(
              RoleTrafficCompetence("EXL", "P"),
              RoleTrafficCompetence("EXL", "R"),
              RoleTrafficCompetence("EXP", "P"),
              RoleTrafficCompetence("EXP", "R"),
              RoleTrafficCompetence("EXT", "P"),
              RoleTrafficCompetence("EXT", "R"),
              RoleTrafficCompetence("PLA", "R"),
              RoleTrafficCompetence("RFC", "R"),
              RoleTrafficCompetence("DIS", "N/A"),
              RoleTrafficCompetence("IPR", "N/A"),
              RoleTrafficCompetence("ENQ", "P"),
              RoleTrafficCompetence("ENQ", "R"),
              RoleTrafficCompetence("ENQ", "N/A"),
              RoleTrafficCompetence("REC", "P"),
              RoleTrafficCompetence("REC", "R"),
              RoleTrafficCompetence("REC", "N/A")
            )
          )
        )
      )
    )
  )

  val mongoJson = Json.obj(
    "phoneNumber"  -> "+45 342234 34543",
    "emailAddress" -> "test@dk",
    "customsOfficeLsd" -> Json.obj(
      "city"                   -> "Hirtshals",
      "languageCode"           -> "en",
      "spaceToAdd"             -> true,
      "customsOfficeUsualName" -> "Hirtshals",
      "prefixSuffixFlag"       -> false,
      "streetAndNumber"        -> "Test 7"
    ),
    "customsOfficeTimetable" -> Json.arr(
      Json.obj(
        "seasonCode"      -> 1,
        "seasonStartDate" -> "2018-01-01",
        "seasonEndDate"   -> "2099-12-31",
        "customsOfficeTimetableLine" -> Json.arr(
          Json.obj(
            "dayInTheWeekEndDay"              -> 5,
            "openingHoursTimeFirstPeriodFrom" -> "08:00:00",
            "dayInTheWeekBeginDay"            -> 1,
            "openingHoursTimeFirstPeriodTo"   -> "16:00:00",
            "customsOfficeRoleTrafficCompetence" -> Json.arr(
              Json.obj(
                "roleName"    -> "EXL",
                "trafficType" -> "P"
              ),
              Json.obj(
                "roleName"    -> "EXL",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "EXP",
                "trafficType" -> "P"
              ),
              Json.obj(
                "roleName"    -> "EXP",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "EXT",
                "trafficType" -> "P"
              ),
              Json.obj(
                "roleName"    -> "EXT",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "PLA",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "RFC",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "DIS",
                "trafficType" -> "N/A"
              ),
              Json.obj(
                "roleName"    -> "IPR",
                "trafficType" -> "N/A"
              ),
              Json.obj(
                "roleName"    -> "ENQ",
                "trafficType" -> "P"
              ),
              Json.obj(
                "roleName"    -> "ENQ",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "ENQ",
                "trafficType" -> "N/A"
              ),
              Json.obj(
                "roleName"    -> "REC",
                "trafficType" -> "P"
              ),
              Json.obj(
                "roleName"    -> "REC",
                "trafficType" -> "R"
              ),
              Json.obj(
                "roleName"    -> "REC",
                "trafficType" -> "N/A"
              )
            )
          )
        )
      )
    ),
    "postalCode" -> "9850",
    "activeFrom" -> Json.obj(
      "$date" -> Json.obj("$numberLong" -> expectedSnapshot.activeFrom.toEpochMilli.toString)
    ),
    "countryCode"                                 -> "DK",
    "customsOfficeSpecificNotesCodes"             -> Json.arr("SN0009"),
    "traderDedicated"                             -> false,
    "referenceNumberCompetentAuthorityOfEnquiry"  -> "DK003102",
    "referenceNumberCompetentAuthorityOfRecovery" -> "DK003102",
    "referenceNumber"                             -> "DK003102"
  )

  "CustomsOffice.fromDpsCustomOfficeList" should "convert a DpsCustomsOffice to the crdl-cache CustomsOffice model" in {

    CustomsOffice.fromDpsCustomOfficeList(inputOffice) mustBe expectedSnapshot
  }

  it should "throw CustomsOfficeDetailMissing error when CustomsOfficeDetail is missing in the input" in {
    val customsOfficeDetailMissing =
      the[CustomsOfficeDetailMissing] thrownBy
        CustomsOffice.fromDpsCustomOfficeList(inputOffice.copy(customsofficelsd = List.empty))
    customsOfficeDetailMissing.referenceNumber mustBe "DK003102"
  }

  "The MongoDB format for CustomsOffice" should "serialize all properties as Mongo Extended JSON" in {
    Json.toJson(expectedSnapshot)(CustomsOffice.mongoFormat) mustBe mongoJson
  }

  it should "deserialize all properties from Mongo Extended JSON" in {
    Json.fromJson[CustomsOffice](mongoJson)(CustomsOffice.mongoFormat).get mustBe expectedSnapshot
  }

  it should "throw InvalidDateFormat error when an invalid date is provided the input" in {
    val invalidDateFormat =
      the[InvalidDateFormat] thrownBy
        CustomsOffice.fromDpsCustomOfficeList(
          inputOffice.copy(rdentrystatus = RDEntryStatus("valid", "xyz"))
        )
    invalidDateFormat.invalidDate mustBe "xyz"
  }
}
