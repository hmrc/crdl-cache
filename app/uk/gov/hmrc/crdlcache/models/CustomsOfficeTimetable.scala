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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.crdlcache.models.CustomsOffice.dateFormat2
import uk.gov.hmrc.crdlcache.models.TimetableLine.fromDpsTimetableLine
import uk.gov.hmrc.crdlcache.models.dps.col.DpsCustomsOfficeTimetable

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class CustomsOfficeTimetable(
  seasonCode: Int, // Is this okay to be taken out of the nested customsofficetimetable, also is this always the number 1// group into customsofficetimetable
  seasonName: Option[String],
  seasonStartDate: LocalDate,
  seasonEndDate: LocalDate,
  customsOfficeTimetableLine: List[TimetableLine]
)

object CustomsOfficeTimetable {
  given format: Format[CustomsOfficeTimetable] = Json.format[CustomsOfficeTimetable]
  def fromDpsCustomsOfficeTimetable(
    dpsCustomsOfficeTimetable: DpsCustomsOfficeTimetable,
    dateFormat: DateTimeFormatter
  ): CustomsOfficeTimetable = {
    CustomsOfficeTimetable(
      dpsCustomsOfficeTimetable.seasoncode.toInt,
      dpsCustomsOfficeTimetable.seasonname,
      LocalDate.parse(dpsCustomsOfficeTimetable.seasonstartdate, dateFormat),
      LocalDate.parse(dpsCustomsOfficeTimetable.seasonenddate, dateFormat),
      dpsCustomsOfficeTimetable.customsofficetimetableline.map(fromDpsTimetableLine)
    )
  }
}
