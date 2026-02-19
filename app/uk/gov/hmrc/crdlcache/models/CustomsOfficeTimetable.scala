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
import uk.gov.hmrc.crdlcache.models.TimetableLine.fromDpsTimetableLine
import uk.gov.hmrc.crdlcache.models.dps.col.DpsCustomsOfficeTimetable
import uk.gov.hmrc.crdlcache.utils.ParserUtils.parseDate

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class CustomsOfficeTimetable(
  seasonCode: Int,
  seasonName: Option[String],
  seasonStartDate: LocalDate,
  seasonEndDate: LocalDate,
  customsOfficeTimetableLine: List[TimetableLine]
)

object CustomsOfficeTimetable {
  def fromDpsCustomsOfficeTimetable(
    dpsCustomsOfficeTimetables: List[DpsCustomsOfficeTimetable],
    dateFormat: DateTimeFormatter
  ): List[CustomsOfficeTimetable] =
    dpsCustomsOfficeTimetables.map { dpsCustomsOfficeTimetable =>
      CustomsOfficeTimetable(
        dpsCustomsOfficeTimetable.seasoncode.toInt,
        dpsCustomsOfficeTimetable.seasonname,
        parseDate(dpsCustomsOfficeTimetable.seasonstartdate, dateFormat),
        parseDate(dpsCustomsOfficeTimetable.seasonenddate, dateFormat),
        dpsCustomsOfficeTimetable.customsofficetimetableline.flatMap(fromDpsTimetableLine)
      )
    }
}
