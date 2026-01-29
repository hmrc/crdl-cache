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
import uk.gov.hmrc.crdlcache.models.CodeListType.{CORRESPONDENCE, PD, STANDARD}

enum CodeListCode(val code: String, val listType: CodeListType = STANDARD) {
  // BC01 (Evidence Types)
  case BC01 extends CodeListCode("BC01")
  // BC03 (Reasons for action not possible)
  case BC03 extends CodeListCode("BC03")
  // BC08 (Country)
  case BC08 extends CodeListCode("BC08")
  // BC09 (Refusal reasons)
  case BC09 extends CodeListCode("BC09")
  // BC11 (National Administrations)
  case BC11 extends CodeListCode("BC11")
  // BC12 (Language Codes)
  case BC12 extends CodeListCode("BC12")
  // BC15 (Event Types)
  case BC15 extends CodeListCode("BC15")
  // BC17 (Packaging Types)
  case BC17 extends CodeListCode("BC17")
  // BC22 (Alert or rejection of movement reasons)
  case BC22 extends CodeListCode("BC22")
  // BC26 (Reasons for interruption)
  case BC26 extends CodeListCode("BC26")
  // BC34 (Event submitting persons)
  case BC34 extends CodeListCode("BC34")
  // BC35 (Transport Units)
  case BC35 extends CodeListCode("BC35")
  // BC36 (Excise Products)
  case BC36 extends CodeListCode("BC36")
  // BC37 (CN Codes)
  case BC37 extends CodeListCode("BC37")
  // BC40 (Wine growing zones)
  case BC40 extends CodeListCode("BC40")
  // BC41 (Wine Operations)
  case BC41 extends CodeListCode("BC41")
  // BC43 (Cancellation reasons)
  case BC43 extends CodeListCode("BC43")
  // BC46 (Unsatisfactory Reasons)
  case BC46 extends CodeListCode("BC46")
  // BC51 (Delay Explanations)
  case BC51 extends CodeListCode("BC51")
  // BC52 (Units Of Measure)
  case BC52 extends CodeListCode("BC52")
  // BC57 (Request Actions)
  case BC57 extends CodeListCode("BC57")
  // BC58 (Reasons For Delayed Result)
  case BC58 extends CodeListCode("BC58")
  // BC66 (Excise Product Categories)
  case BC66 extends CodeListCode("BC66")
  // BC67 (Transport Modes)
  case BC67 extends CodeListCode("BC67")
  // BC106 (Document Types)
  case BC106 extends CodeListCode("BC106")
  // BC107 (Manual Closure Request Reasons)
  case BC107 extends CodeListCode("BC107")
  // BC108 (Manual Closure Rejection Reasons)
  case BC108 extends CodeListCode("BC108")
  // BC109 (National Administration Degrees Plato)
  case BC109 extends CodeListCode("BC109")
  // CL141 (Customs Offices)
  case CL141 extends CodeListCode("CL141")
  // CL008 (Country Codes Full List)
  case CL008 extends CodeListCode("CL008", listType = PD)
  // CL009 (Country Codes Common Transit)
  case CL009 extends CodeListCode("CL009", listType = PD)
  // CL010 (Country Codes Community)
  case CL010 extends CodeListCode("CL010", listType = PD)
  case CL016 extends CodeListCode("CL016", listType = PD)
  // CL017
  case CL017 extends CodeListCode("CL017", listType = PD)
  // CL019
  case CL019 extends CodeListCode("CL019", listType = PD)
  // CL030
  case CL030 extends CodeListCode("CL030", listType = PD)
  // CL038
  case CL038 extends CodeListCode("CL038", listType = PD)
  // CL042
  case CL042 extends CodeListCode("CL042", listType = PD)
  // CL048
  case CL048 extends CodeListCode("CL048", listType = PD)
  // CL056
  case CL056 extends CodeListCode("CL056", listType = PD)
  // CL076
  case CL076 extends CodeListCode("CL076", listType = PD)
  // CL094
  case CL094 extends CodeListCode("CL094", listType = PD)
  case CL101 extends CodeListCode("CL101", listType = PD)
  // CL112
  case CL112 extends CodeListCode("CL112", listType = PD)
  // CL116
  case CL116 extends CodeListCode("CL116", listType = PD)
  // CL147
  case CL147 extends CodeListCode("CL147", listType = PD)
  case CL152 extends CodeListCode("CL152", listType = PD)
  // CL165
  case CL165 extends CodeListCode("CL165", listType = PD)
  // CL167
  case CL167 extends CodeListCode("CL167", listType = PD)
  // CL178
  case CL178 extends CodeListCode("CL178", listType = PD)
  // CL180
  case CL180 extends CodeListCode("CL180", listType = PD)
  // CL181
  case CL181 extends CodeListCode("CL181", listType = PD)
  // CL182
  case CL182 extends CodeListCode("CL182", listType = PD)
  // CL190
  case CL190 extends CodeListCode("CL190", listType = PD)
  // CL198
  case CL198 extends CodeListCode("CL198", listType = PD)
  // CL213
  case CL213 extends CodeListCode("CL213", listType = PD)
  // CL214
  case CL214 extends CodeListCode("CL214", listType = PD)
  // CL215
  case CL215 extends CodeListCode("CL215", listType = PD)
  // CL217
  case CL217 extends CodeListCode("CL217", listType = PD)
  // CL218
  case CL218 extends CodeListCode("CL218", listType = PD)
  // CL219
  case CL219 extends CodeListCode("CL219", listType = PD)
  // CL226
  case CL226 extends CodeListCode("CL226", listType = PD)
  // CL228
  case CL228 extends CodeListCode("CL228", listType = PD)
  // CL229
  case CL229 extends CodeListCode("CL229", listType = PD)
  // CL230 (Guarantee Type EU Non TIR)
  case CL230 extends CodeListCode("CL230", listType = PD)
  // CL231 (Declaration Type)
  case CL231 extends CodeListCode("CL231", listType = PD)
  // CL232
  case CL232 extends CodeListCode("CL232", listType = PD)
  // CL234 (Document Type Excise)
  case CL234 extends CodeListCode("CL234", listType = PD)
  // CL235
  case CL235 extends CodeListCode("CL235", listType = PD)
  // CL236
  case CL236 extends CodeListCode("CL236", listType = PD)
  // CL239 (Additional Information)
  case CL239 extends CodeListCode("CL239", listType = PD)
  case CL244 extends CodeListCode("CL244", listType = PD)
  // CL248
  case CL248 extends CodeListCode("CL248", listType = PD)
  // CL251
  case CL251 extends CodeListCode("CL251", listType = PD)
  // CL252
  case CL252 extends CodeListCode("CL252", listType = PD)
  // CL286
  case CL286 extends CodeListCode("CL286", listType = PD)
  // CL296
  case CL296 extends CodeListCode("CL296", listType = PD)
  // CL326
  case CL326 extends CodeListCode("CL326", listType = PD)
  // CL347
  case CL347 extends CodeListCode("CL347", listType = PD)
  // CL349
  case CL349 extends CodeListCode("CL349", listType = PD)
  // CL380 (Additional Reference)
  case CL380 extends CodeListCode("CL380", listType = PD)
  // CL437
  case CL437 extends CodeListCode("CL437", listType = PD)
  case CL505 extends CodeListCode("CL505", listType = PD)
  // CL560
  case CL560 extends CodeListCode("CL560", listType = PD)
  // CL580
  case CL580 extends CodeListCode("CL580", listType = PD)
  // CL581
  case CL581 extends CodeListCode("CL581", listType = PD)
  // CL704
  case CL704 extends CodeListCode("CL704", listType = PD)
  // CL716
  case CL716 extends CodeListCode("CL716", listType = PD)
  // CL750
  case CL750 extends CodeListCode("CL750", listType = PD)
  // CL752
  case CL752 extends CodeListCode("CL752", listType = PD)
  // CL754
  case CL754 extends CodeListCode("CL754", listType = PD)
  // E200 (CN Code <-> Excise Products Correspondence)
  case E200 extends CodeListCode("E200", listType = CORRESPONDENCE)
  // Unknown codelist code
  case Unknown(override val code: String) extends CodeListCode(code)
}

object CodeListCode {
  private val values: Set[CodeListCode] =
    Set(
      BC01,
      BC03,
      BC08,
      BC09,
      BC11,
      BC12,
      BC15,
      BC17,
      BC22,
      BC26,
      BC34,
      BC35,
      BC36,
      BC37,
      BC40,
      BC41,
      BC43,
      BC46,
      BC51,
      BC52,
      BC57,
      BC58,
      BC66,
      BC67,
      BC106,
      BC107,
      BC108,
      BC109,
      CL008,
      CL009,
      CL010,
      CL016,
      CL017,
      CL019,
      CL030,
      CL038,
      CL042,
      CL048,
      CL056,
      CL076,
      CL094,
      CL101,
      CL112,
      CL116,
      CL141,
      CL147,
      CL152,
      CL165,
      CL167,
      CL178,
      CL180,
      CL181,
      CL182,
      CL190,
      CL198,
      CL213,
      CL214,
      CL215,
      CL217,
      CL218,
      CL219,
      CL226,
      CL228,
      CL229,
      CL230,
      CL231,
      CL232,
      CL234,
      CL235,
      CL236,
      CL239,
      CL244,
      CL248,
      CL251,
      CL252,
      CL286,
      CL296,
      CL326,
      CL347,
      CL349,
      CL380,
      CL437,
      CL505,
      CL560,
      CL580,
      CL581,
      CL704,
      CL716,
      CL750,
      CL752,
      CL754,
      E200
    )

  private val codes: Map[String, CodeListCode] = values.map(value => value.code -> value).toMap

  def fromString(code: String): CodeListCode = codes.getOrElse(code, Unknown(code))

  given Format[CodeListCode] = Format.of[String].bimap(codes.withDefault(Unknown.apply), _.code)

  given PathBindable[CodeListCode] = new PathBindable.Parsing[CodeListCode](
    value => codes.getOrElse(value, throw new IllegalArgumentException("Unknown code list code")),
    _.code,
    (code, e) => s"Cannot parse parameter $code as CodeListCode: ${e.getMessage}"
  )
}
