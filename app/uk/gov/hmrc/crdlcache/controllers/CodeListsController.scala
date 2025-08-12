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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCodeLists
import uk.gov.hmrc.crdlcache.models.CodeListType.{CORRESPONDENCE, STANDARD}
import uk.gov.hmrc.crdlcache.models.formats.HttpFormats
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.repositories.{
  CorrespondenceListsRepository,
  LastUpdatedRepository,
  StandardCodeListsRepository
}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class CodeListsController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  codeListsRepository: StandardCodeListsRepository,
  correspondenceListsRepository: CorrespondenceListsRepository,
  lastUpdatedRepository: LastUpdatedRepository,
  clock: Clock
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with HttpFormats {

  def fetchCodeListEntries(
    codeListCode: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, JsValue]],
    activeAt: Option[Instant]
  ): Action[AnyContent] =
    auth.authorizedAction(ReadCodeLists).async { _ =>
      val codeListEntries = codeListCode.listType match {
        case STANDARD =>
          codeListsRepository
            .fetchEntries(
              codeListCode,
              filterKeys,
              filterProperties,
              activeAt.getOrElse(clock.instant())
            )
        case CORRESPONDENCE =>
          correspondenceListsRepository
            .fetchEntries(
              codeListCode,
              filterKeys,
              filterProperties,
              activeAt.getOrElse(clock.instant())
            )
      }

      codeListEntries.map { entries =>
        Ok(Json.toJson(entries))
      }
    }

  def fetchCodeListVersions: Action[AnyContent] =
    auth.authorizedAction(ReadCodeLists).async { _ =>
      lastUpdatedRepository.fetchAllLastUpdated
        .map { lastUpdatedEntries =>
          Ok(Json.toJson(lastUpdatedEntries))
        }
    }
}
