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
  case BC08 extends CodeListCode("BC08")
  // BC11 (National Administrations)
  case BC11 extends CodeListCode("BC11")
  // BC17 (Packaging Types)
  case BC17 extends CodeListCode("BC17")
  // BC35 (Transport Units)
  case BC35 extends CodeListCode("BC35")
  // BC36 (Excise Products)
  case BC36 extends CodeListCode("BC36")
  // BC37 (CN Codes)
  case BC37 extends CodeListCode("BC37")
  // BC41 (Wine Operations)
  case BC41 extends CodeListCode("BC41")
  // BC66 (Excise Product Categories)
  case BC66 extends CodeListCode("BC66")
  // BC106 (Document Types)
  case BC106 extends CodeListCode("BC106")
  // BC109 (National Administration Degrees Plato)
  case BC109 extends CodeListCode("BC109")
  // CL141 (Customs Offices)
  case CL141 extends CodeListCode("CL141")
  // Unknown codelist code
  case Unknown(override val code: String) extends CodeListCode(code)
}

object CodeListCode {
  private val values: Set[CodeListCode] =
    Set(BC08, BC11, BC17, BC35, BC36, BC37, BC41, BC66, BC106, BC109, CL141)

  private val codes: Map[String, CodeListCode] = values.map(value => value.code -> value).toMap

  def fromString(code: String): CodeListCode = codes.getOrElse(code, Unknown(code))

  given Format[CodeListCode] = Format.of[String].bimap(codes.withDefault(Unknown.apply), _.code)

  given PathBindable[CodeListCode] = new PathBindable.Parsing[CodeListCode](
    value => codes.getOrElse(value, throw new IllegalArgumentException("Unknown code list code")),
    _.code,
    (code, e) => s"Cannot parse parameter $code as CodeListCode: ${e.getMessage}"
  )
}
