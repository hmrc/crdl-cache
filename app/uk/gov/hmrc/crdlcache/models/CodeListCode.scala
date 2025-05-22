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

sealed abstract class CodeListCode(val code: String) extends Product with Serializable {}

object CodeListCode {
  case object BC08                              extends CodeListCode("BC08")
  case class Unknown(override val code: String) extends CodeListCode(code)

  private val values: Set[CodeListCode]        = Set(BC08)
  private val codes: Map[String, CodeListCode] = values.map(value => value.code -> value).toMap
  given Format[CodeListCode] = Format.of[String].bimap(codes.withDefault(Unknown.apply), _.code)
}
