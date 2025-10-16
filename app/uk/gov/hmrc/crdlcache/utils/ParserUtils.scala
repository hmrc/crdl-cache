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

package uk.gov.hmrc.crdlcache.utils

import uk.gov.hmrc.crdlcache.models.errors.ImportError.{
  InvalidDateFormat,
  InvalidDayFormat,
  InvalidTimeFormat
}

import java.time.{DayOfWeek, Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.time.format.{DateTimeFormatter, DateTimeParseException}

object ParserUtils {
  def parseDateToInstant(value: String, formatter: DateTimeFormatter): Instant = {
    try
      formatter.parseBest(value, LocalDateTime.from, LocalDate.from) match {
        case ldt: LocalDateTime => ldt.toInstant(ZoneOffset.UTC)
        case ld: LocalDate      => ld.atStartOfDay(ZoneOffset.UTC).toInstant
      }
    catch case ex: DateTimeParseException => throw InvalidDateFormat(value)
  }

  def parseDate(value: String, dateFormat: DateTimeFormatter): LocalDate = {
    try LocalDate.parse(value, dateFormat)
    catch case ex: DateTimeParseException => throw InvalidDateFormat(value)
  }

  def parseTime(value: String, timeFormat: DateTimeFormatter): LocalTime = {
    try LocalTime.parse(value, timeFormat)
    catch case ex: DateTimeParseException => throw InvalidTimeFormat(value)
  }

  def parseDayOfWeek(value: String): DayOfWeek = {
    try DayOfWeek.of(value.toInt)
    catch case ex: Exception => throw InvalidDayFormat(value)
  }
}
