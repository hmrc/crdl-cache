/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.crdlcache.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCodeLists
import uk.gov.hmrc.crdlcache.models.formats.HttpFormats
import uk.gov.hmrc.crdlcache.repositories.LastUpdatedRepository
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.crdlcache.models.paging.PagedResult

import scala.concurrent.ExecutionContext

@Singleton()
class CodeListsV2Controller @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  lastUpdatedRepository: LastUpdatedRepository
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with HttpFormats {

  def fetchCodeListVersions(pageNum: Int, pageSize: Int): Action[AnyContent] =
    auth.authorizedAction(ReadCodeLists).async { _ =>
      val listUpdatedFuture   = lastUpdatedRepository.fetchAllLastUpdatedV2(pageNum, pageSize)
      val codeListCountFuture = lastUpdatedRepository.codeListCount()
      for {
        listUpdated   <- listUpdatedFuture
        codeListCount <- codeListCountFuture
      } yield {
        Ok(
          Json.toJson(
            PagedResult(
              items = listUpdated,
              pageNum = pageNum,
              pageSize = pageSize,
              itemsInPage = listUpdated.length,
              totalItems = codeListCount,
              totalPages = Math.ceil(codeListCount.toFloat / pageSize).toInt
            )
          )
        )
      }
    }
}
