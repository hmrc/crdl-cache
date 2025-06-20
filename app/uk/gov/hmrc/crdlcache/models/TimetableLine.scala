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

case class TimetableLine(
  dayInTheWeekBeginDay: Int,
  openingHoursTimeFirstPeriodFrom: Int,//what abut 0800 will it be stored as 800 in mongo?
  openingHoursTimeFirstPeriodTo: Int,
  dayInTheWeekEndDay: Int,
  openingHoursTimeSecondPeriodFrom: Option[Int],
  openingHoursTimeSecondPeriodTo: Option[Int],
  customsOfficeRoleTrafficCompetence: List[RoleTrafficCompetence]
)
object TimetableLine {
  given format: Format[TimetableLine] = Json.format[TimetableLine]
  def fromDpsTimetableLine(timetableLine: DpsTimetableLine): TimetableLine = {
    TimetableLine(
      timetableLine.dayintheweekbeginday.toInt,
      timetableLine.openinghourstimefirstperiodfrom.toInt,
      timetableLine.openinghourstimefirstperiodto.toInt,
      timetableLine.dayintheweekendday.toInt,
      timetableLine.openinghourstimesecondperiodfrom.map(_.toInt),
      timetableLine.openinghourstimesecondperiodto.map(_.toInt),
      timetableLine.customsofficeroletrafficcompetence.map(fromDpsRoleTrafficCompetence)
    )
  }
}
