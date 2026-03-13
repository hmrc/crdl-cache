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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCodeLists
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.models.CodeListType.{CORRESPONDENCE, PD, STANDARD}
import uk.gov.hmrc.crdlcache.models.formats.HttpFormats
import uk.gov.hmrc.crdlcache.models.paging.PagedResult
import uk.gov.hmrc.crdlcache.repositories.{CorrespondenceListsRepository, StandardCodeListsRepository}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class CodeListEntriesV2Controller @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  codeListsRepository: StandardCodeListsRepository,
  correspondenceListsRepository: CorrespondenceListsRepository,
  clock: Clock
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with HttpFormats {

  def fetchCodeListEntries(
    code: CodeListCode,
    pageNum: Int,
    pageSize: Int,
    activeAt: Option[Instant],
    key: Option[String],
    value: Option[String]
  ): Action[AnyContent] = auth.authorizedAction(ReadCodeLists).async { _ =>
    val resolvedActiveAt = activeAt.getOrElse(clock.instant())
    val repo = code.listType match {
      case CORRESPONDENCE => correspondenceListsRepository
      case STANDARD | PD  => codeListsRepository
    }
    val entriesFuture = repo.fetchEntriesPaged(code, resolvedActiveAt, pageNum, pageSize, key, value)
    val countFuture   = repo.countEntries(code, resolvedActiveAt, key, value)
    for {
      entries <- entriesFuture
      count   <- countFuture
    } yield {
      Ok(
        Json.toJson(
          PagedResult(
            items = entries,
            pageNum = pageNum,
            pageSize = pageSize,
            itemsInPage = entries.length,
            totalItems = count,
            totalPages = Math.ceil(count.toFloat / pageSize).toInt
          )
        )
      )
    }
  }
}
