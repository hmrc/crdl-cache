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
import uk.gov.hmrc.crdlcache.models.TimetableLine.timeFormat

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, LocalDate, LocalTime}

class CustomsOfficeSpec extends AnyFlatSpec with Matchers with TestData {
    "CustomsOffice.fromDpsCustomOfficeList" should "convert a DpsCustomsOffice to the crdl-cache CustomsOffice model" in {
    val inputOffice = DK003102
    val dateFormat  = DateTimeFormatter.ofPattern("yyyyMMdd")
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
        "Hirtshals Toldekspedition",
        "DA",
        "Hirtshals",
        false,
        None,
        None,
        false,
        "Dalsagervej 7"
      ),
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

    CustomsOffice.fromDpsCustomOfficeList(inputOffice) mustBe expectedSnapshot
  }

}
