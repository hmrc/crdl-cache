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

package uk.gov.hmrc.crdlcache.controllers.testonly

import org.mongodb.scala.*
import org.mongodb.scala.model.Filters
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcache.repositories.{
  CorrespondenceListsRepository,
  LastUpdatedRepository,
  StandardCodeListsRepository,
  CustomsOfficeListsRepository
}
import uk.gov.hmrc.crdlcache.schedulers.JobScheduler
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestOnlyController @Inject() (
  cc: ControllerComponents,
  jobScheduler: JobScheduler,
  lastUpdatedRepository: LastUpdatedRepository,
  codeListsRepository: StandardCodeListsRepository,
  correspondenceListsRepository: CorrespondenceListsRepository,
  customsOfficeListsRepository: CustomsOfficeListsRepository
)(using ec: ExecutionContext)
  extends BackendController(cc) {

  def importCodeLists(): Action[AnyContent] = Action {
    jobScheduler.startCodeListImport()
    Accepted
  }

  def codeListImportStatus(): Action[AnyContent] = Action {
    Ok(Json.toJson(jobScheduler.codeListImportStatus()))
  }

  def importPhaseAndDomainLists(): Action[AnyContent] = Action {
    jobScheduler.startPhaseAndDomainListImport()
    Accepted
  }

  def phaseAndDomainListImportStatus(): Action[AnyContent] = Action {
    Ok(Json.toJson(jobScheduler.phaseAndDomainListImportStatus()))
  }

  def importCorrespondenceLists(): Action[AnyContent] = Action {
    jobScheduler.startCorrespondenceListImport()
    Accepted
  }

  def correspondenceListImportStatus(): Action[AnyContent] = Action {
    Ok(Json.toJson(jobScheduler.codeListImportStatus()))
  }

  def deleteCodeLists(): Action[AnyContent] = Action.async {
    codeListsRepository.collection.deleteMany(Filters.empty()).toFuture().map {
      case result if result.wasAcknowledged() => Ok
      case _                                  => InternalServerError
    }
  }

  def deletePhaseAndDomainLists(): Action[AnyContent] = Action.async {
    codeListsRepository.collection.deleteMany(Filters.regex("codeListCode", "^CL")).toFuture().map {
      case result if result.wasAcknowledged() => Ok
      case _                                  => InternalServerError
    }
  }

  def deleteCorrespondenceLists(): Action[AnyContent] = Action.async {
    correspondenceListsRepository.collection.deleteMany(Filters.empty()).toFuture().map {
      case result if result.wasAcknowledged() => Ok
      case _                                  => InternalServerError
    }
  }

  def deleteLastUpdated(): Action[AnyContent] = Action.async {
    lastUpdatedRepository.collection.deleteMany(Filters.empty()).toFuture().map {
      case result if result.wasAcknowledged() => Ok
      case _                                  => InternalServerError
    }
  }

  def officesImportStatus(): Action[AnyContent] = Action {
    Ok(Json.toJson(jobScheduler.customsOfficeImportStatus()))
  }

  def deleteCustomsOfficeLists(): Action[AnyContent] = Action.async {
    customsOfficeListsRepository.collection.deleteMany(Filters.empty()).toFuture().map {
      case result if result.wasAcknowledged() => Ok
      case _                                  => InternalServerError
    }
  }

  def importCustomsOfficeLists(): Action[AnyContent] = Action {
    jobScheduler.startCustomsOfficeListImport()
    Accepted
  }

}
