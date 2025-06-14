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

package uk.gov.hmrc.crdlcache.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.crdlcache.repositories.CodeListsRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class CodeListsController @Inject() (
  cc: ControllerComponents,
  codeListsRepository: CodeListsRepository,
  clock: Clock
)(using ec: ExecutionContext)
  extends BackendController(cc) {

  def fetchCodeListEntries(
    codeListCode: CodeListCode,
    filterKeys: Option[Set[String]],
    activeAt: Option[Instant]
  ): Action[AnyContent] = Action.async { _ =>
    codeListsRepository
      .fetchCodeListEntries(codeListCode, filterKeys, activeAt.getOrElse(clock.instant()))
      .map { entries =>
        Ok(Json.toJson(entries))
      }
  }
}
