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

import uk.gov.hmrc.crdlcache.models.RoleTrafficCompetence.fromDpsRoleTrafficCompetence
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.crdlcache.models.dps.col.DpsTimetableLine
import uk.gov.hmrc.crdlcache.models.formats.JavaTimeFormats
import uk.gov.hmrc.crdlcache.utils.ParserUtils.{parseDayOfWeek, parseTime}

import java.time.{DayOfWeek, LocalTime}

case class TimetableLine(
  dayInTheWeekBeginDay: DayOfWeek,
  openingHoursTimeFirstPeriodFrom: LocalTime,
  openingHoursTimeFirstPeriodTo: LocalTime,
  dayInTheWeekEndDay: DayOfWeek,
  openingHoursTimeSecondPeriodFrom: Option[LocalTime],
  openingHoursTimeSecondPeriodTo: Option[LocalTime],
  customsOfficeRoleTrafficCompetence: List[RoleTrafficCompetence]
)
object TimetableLine extends JavaTimeFormats {

  given format: Format[TimetableLine] = Json.format[TimetableLine]

  def fromDpsTimetableLine(timetableLine: DpsTimetableLine): Option[TimetableLine] = {
    for {
      beginDay            <- timetableLine.dayintheweekbeginday
      endDay              <- timetableLine.dayintheweekendday
      firstPeriodOpenFrom <- timetableLine.openinghourstimefirstperiodfrom
      firstPeriodOpenTo   <- timetableLine.openinghourstimefirstperiodto
      secondPeriodOpenFrom = timetableLine.openinghourstimesecondperiodfrom
      secondPeriodOpenTo   = timetableLine.openinghourstimesecondperiodto
      roleTraffic <- timetableLine.customsofficeroletrafficcompetence
    } yield {
      TimetableLine(
        parseDayOfWeek(beginDay),
        parseTime(firstPeriodOpenFrom, timeFormat),
        parseTime(firstPeriodOpenTo, timeFormat),
        parseDayOfWeek(endDay),
        secondPeriodOpenFrom.map(parseTime(_, timeFormat)),
        secondPeriodOpenTo.map(parseTime(_, timeFormat)),
        roleTraffic.map(fromDpsRoleTrafficCompetence)
      )
    }
  }
}
