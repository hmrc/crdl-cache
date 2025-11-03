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

package uk.gov.hmrc.crdlcache.models.paging

import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.crdlcache.models.CustomsOfficeSummary
import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber

final case class PagedResult[T](
  items: Seq[T],
  pageNum: Int,
  pageSize: Int,
  itemsInPage: Int,
  totalItems: Long,
  totalPages: Int
)

object PagedResult {
  implicit def pagedResultWrites[T](implicit writesT: Writes[T]): Writes[PagedResult[T]] =
    new Writes[PagedResult[T]] {
      def writes(result: PagedResult[T]) = JsObject(
        Seq(
          "items"       -> JsArray(result.items.map(Json.toJson(_))),
          "pageNum"     -> JsNumber(result.pageNum),
          "pageSize"    -> JsNumber(result.pageSize),
          "itemsInPage" -> JsNumber(result.itemsInPage),
          "totalItems"  -> JsNumber(result.totalItems),
          "totalPages"  -> JsNumber(result.totalPages)
        )
      )
    }
}
