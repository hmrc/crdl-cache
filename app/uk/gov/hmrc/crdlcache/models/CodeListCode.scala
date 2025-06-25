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

import play.api.libs.json.Format
import play.api.mvc.PathBindable

enum CodeListCode(val code: String) {
  // BC08 (Country)
  case BC08                               extends CodeListCode("BC08")
  // BC36 (Excise Products)
  case BC36                               extends CodeListCode("BC36")
  // BC46 (Unsatisfactory Reasons)
  case BC46                               extends CodeListCode("BC46")
  // BC51 (Delay Explanations)
  case BC51                               extends CodeListCode("BC51")
  // BC52 (Units Of Measure)
  case BC52                               extends CodeListCode("BC52")
  // BC57 (Request Actions)
  case BC57                               extends CodeListCode("BC57")
  // BC58 (Reasons For Delayed Result)
  case BC58                               extends CodeListCode("BC58")
  // BC66 (Excise Products Category)
  case BC66                               extends CodeListCode("BC66")
  // BC67 (Transport Modes)
  case BC67                               extends CodeListCode("BC67")
  // BC107 (Manual Closure Request Reasons)
  case BC107                               extends CodeListCode("BC107")
  // BC108 (Manual Closure Rejection Reasons)
  case BC108                               extends CodeListCode("BC108")
  // CL141 (Customs Offices)
  case CL141                              extends CodeListCode("CL141")
  // Unknown codelist code
  case Unknown(override val code: String) extends CodeListCode(code)
}

object CodeListCode {
  private val values: Set[CodeListCode]        = Set(BC08, BC46, BC51, BC52, BC57, BC58, BC66, BC67, BC107, BC108, CL141)
  private val codes: Map[String, CodeListCode] = values.map(value => value.code -> value).toMap

  def fromString(code: String): CodeListCode = codes.getOrElse(code, Unknown(code))

  given Format[CodeListCode] = Format.of[String].bimap(codes.withDefault(Unknown.apply), _.code)

  given PathBindable[CodeListCode] = new PathBindable.Parsing[CodeListCode](
    value => codes.getOrElse(value, throw new IllegalArgumentException("Unknown code list code")),
    _.code,
    (code, e) => s"Cannot parse parameter $code as CodeListCode: ${e.getMessage}"
  )
}
