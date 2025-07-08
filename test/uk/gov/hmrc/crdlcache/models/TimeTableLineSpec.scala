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
import uk.gov.hmrc.crdlcache.models.errors.ImportError.{InvalidDayFormat, InvalidTimeFormat}

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalTime}

class TimeTableLineSpec extends AnyFlatSpec with Matchers with TestData {
  val inputTimeTableLine = DK003102.customsofficetimetable.customsofficetimetableline

  "TimeTableLine.fromDpsTimetableLine" should "convert a DpsTimetableLine to the crdl-cacle TimetableLine model" in {
    val timeTableLine = inputTimeTableLine.head.copy(
      openinghourstimesecondperiodfrom = Some("0900"),
      openinghourstimesecondperiodto = Some("1700")
    )
    val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
    val expectedTimeTableLine = TimetableLine(
      DayOfWeek.of(1),
      LocalTime.parse("0800", timeFormat),
      LocalTime.parse("1600", timeFormat),
      DayOfWeek.of(5),
      Some(LocalTime.parse("0900", timeFormat)),
      Some(LocalTime.parse("1700", timeFormat)),
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
    TimetableLine.fromDpsTimetableLine(timeTableLine) mustBe Some(expectedTimeTableLine)
  }

  it should "throw InvalidDateFormat error when an invalid date is provided the input" in {
    val invalidTimeFormat =
      the[InvalidTimeFormat] thrownBy
        TimetableLine.fromDpsTimetableLine(
          inputTimeTableLine.head.copy(openinghourstimefirstperiodfrom = Some("test"))
        )
    invalidTimeFormat.invalidTime mustBe "test"
  }

  it should "throw InvalidDayFormat error when an invalid value for day is provided" in {
    val invalidDayFormat = the[InvalidDayFormat] thrownBy
      TimetableLine.fromDpsTimetableLine(
        inputTimeTableLine.head.copy(dayintheweekbeginday = Some("invalidDay"))
      )
    invalidDayFormat.invalidDay mustBe "invalidDay"
  }
}
