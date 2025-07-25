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

package uk.gov.hmrc.crdlcache.models.dps.codelist

import play.api.libs.json.{Json, Reads}
import DataItem.uncapitalize

case class DataItem(dataitem_name: String, dataitem_value: Option[String]) {
  lazy val propertyName: String = uncapitalize(dataitem_name.split('_').last)
}

object DataItem {
  given Reads[DataItem] = Json.reads[DataItem]

  private def uncapitalize(s: String) =
    if (s == null || s.isEmpty || !s.charAt(0).isUpper) s
    else s.updated(0, s.charAt(0).toLower)
}
