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

package uk.gov.hmrc.crdlcache.config

import com.typesafe.config.Config
import play.api.Configuration
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListOrigin}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration) extends ServicesConfig(config) {
  val appName: String = config.get[String]("appName")

  val dpsUrl: String         = baseUrl("dps-api")
  val dpsRefDataPath: String = config.get[String]("microservice.services.dps-api.ref-data-path")
  val dpsCustomsOfficesPath: String =
    config.get[String]("microservice.services.dps-api.customs-offices-path")
  val dpsClientId: String     = config.get[String]("microservice.services.dps-api.clientId")
  val dpsClientSecret: String = config.get[String]("microservice.services.dps-api.clientSecret")

  val importCodeListsSchedule: String           = config.get[String]("import-codelists.schedule")
  val importPhaseAndDomainListsSchedule: String = config.get[String]("import-pd-lists.schedule")
  val importOfficesSchedule: String             = config.get[String]("import-offices.schedule")
  val importCorrespondenceListsSchedule: String =
    config.get[String]("import-correspondence-lists.schedule")

  val defaultLastUpdated: LocalDate =
    LocalDate.parse(config.get[String]("last-updated-date.default"))

  def loadListConfigs[T](configPath: String)(mapper: Config => T): List[T] = {
    val listElementName = configPath.split("import-").last
    config
      .get[Seq[Config]](s"$configPath.$listElementName")
      .toList
      .map(mapper)
  }

  val codeListConfigs: List[CodeListConfig] =
    loadListConfigs("import-codelists") { codeListConfig =>
      CodeListConfig(
        CodeListCode.fromString(codeListConfig.getString("code")),
        CodeListOrigin.valueOf(codeListConfig.getString("origin")),
        codeListConfig.getString("keyProperty")
      )
    }.toList

  val phaseAndDomainListConfigs: List[PhaseAndDomainListConfig] =
    loadListConfigs("import-pd-lists") { phaseAndDomainListConfig =>
      PhaseAndDomainListConfig(
        CodeListCode.fromString(phaseAndDomainListConfig.getString("code")),
        CodeListOrigin.valueOf(phaseAndDomainListConfig.getString("origin")),
        phaseAndDomainListConfig.getString("keyProperty"),
        Some(config.get[String]("import-pd-lists.phase")),
        Some(config.get[String]("import-pd-lists.domain"))
      )
    }.toList

  val correspondenceListConfigs: List[CorrespondenceListConfig] =
    loadListConfigs("import-correspondence-lists") { correspondenceListConfig =>
      val phase: Option[String] = {
        if (correspondenceListConfig.hasPath("phase"))
          Some(correspondenceListConfig.getString("phase"))
        else None
      }
      val domain: Option[String] = {
        if (correspondenceListConfig.hasPath("domain"))
          Some(correspondenceListConfig.getString("domain"))
        else None
      }
      CorrespondenceListConfig(
        CodeListCode.fromString(correspondenceListConfig.getString("code")),
        CodeListOrigin.valueOf(correspondenceListConfig.getString("origin")),
        correspondenceListConfig.getString("keyProperty"),
        correspondenceListConfig.getString("valueProperty"),
        phase,
        domain
      )
    }.toList

}
