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

package uk.gov.hmrc.crdlcache.models.dps

import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.crdlcache.models.CodeListCode

import java.net.URI

case class CodeListResponse(elements: List[CodeListSnapshot], links: List[Relation])

object CodeListResponse {
  given Reads[CodeListResponse] = Json.reads[CodeListResponse]
}

case class CodeListSnapshot(
  code_list_code: CodeListCode,
  code_list_name: String,
  snapshotversion: Int,
  rdentry: List[CodeListEntry]
)

object CodeListSnapshot {
  given Reads[CodeListSnapshot] = Json.reads[CodeListSnapshot]
}

case class CodeListEntry(dataitem: List[DataItem], language: List[LanguageDescription])

object CodeListEntry {
  given Reads[CodeListEntry] = Json.reads[CodeListEntry]
}

case class DataItem(dataitem_name: String, dataitem_value: Option[String])

object DataItem {
  given Reads[DataItem] = Json.reads[DataItem]
}

case class LanguageDescription(lang_code: String, lang_desc: String)

object LanguageDescription {
  given Reads[LanguageDescription] = Json.reads[LanguageDescription]
}

case class Relation(rel: RelationType, href: String)

object Relation {
  given Reads[Relation] = Json.reads[Relation]
}

sealed abstract class RelationType(val name: String) extends Product with Serializable {}

object RelationType {
  case object Self extends RelationType("self")
  case object Prev extends RelationType("prev")
  case object Next extends RelationType("next")
  case class Unknown(override val name: String) extends RelationType(name)

  private val values: Set[RelationType]        = Set(Self, Next, Prev)
  private val names: Map[String, RelationType] = values.map(value => value.name -> value).toMap
  given Reads[RelationType] = Reads.of[String].map(names.withDefault(Unknown.apply))
}
