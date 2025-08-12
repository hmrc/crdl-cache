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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.crdlcache.repositories.CustomsOfficeListsRepository
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.controllers.auth.Permissions.ReadCustomsOfficeLists
import uk.gov.hmrc.crdlcache.models.formats.HttpFormats
import uk.gov.hmrc.internalauth.client.*

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class CustomsOfficeListsController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  customsOfficeListsRepository: CustomsOfficeListsRepository,
  clock: Clock
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with HttpFormats {
  def fetchCustomsOfficeLists(
    referenceNumbers: Option[Set[String]],
    countryCodes: Option[Set[String]],
    roles: Option[Set[String]],
    activeAt: Option[Instant]
  ): Action[AnyContent] = auth.authorizedAction(ReadCustomsOfficeLists).async { _ =>
    customsOfficeListsRepository
      .fetchCustomsOfficeLists(
        referenceNumbers,
        countryCodes,
        roles,
        activeAt.getOrElse(clock.instant())
      )
      .map { customsOfficeLists =>
        Ok(Json.toJson(customsOfficeLists))
      }
  }
}
