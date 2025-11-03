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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{Json, JsValue}
import play.api.libs.json.JsArray

class PagedResultSpec extends AnyFlatSpec with Matchers {
  val defaultItems: Seq[Int] = Seq(1, 2, 3, 4, 5)
  val defaultPagedResult: PagedResult[Int] = PagedResult(
    items = defaultItems,
    pageNum = 1,
    pageSize = defaultItems.length,
    totalItems = defaultItems.length * 10,
    itemsInPage = defaultItems.length,
    totalPages = 10
  )

  val expectedJsonResult: JsValue = Json.obj(
    "items"       -> defaultItems,
    "pageNum"     -> 1,
    "pageSize"    -> 5,
    "totalItems"  -> 50,
    "itemsInPage" -> 5,
    "totalPages"  -> 10
  )

  "The default Writes for PagedResult" should "serilaize with all expected values" in {
    Json.toJson(defaultPagedResult) mustBe expectedJsonResult
  }
}
