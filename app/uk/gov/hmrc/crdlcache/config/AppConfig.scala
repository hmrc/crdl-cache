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
import uk.gov.hmrc.crdlcache.models.{CodeListCode, CodeListOrigin, DomainAndPhase, DomainAndPhaseJob}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.ListHasAsScala

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
//  val importPhaseAndDomainListsSchedule: String = config.get[String]("import-pd-lists.schedule")
  val importOfficesSchedule: String             = config.get[String]("import-offices.schedule")
  val importCorrespondenceListsSchedule: String =
    config.get[String]("import-correspondence-lists.schedule")

  val importOfficesJobDomainAndPhase: DomainAndPhase =
    DomainAndPhase(
      config.get[Option[String]]("import-offices.domain"),
      config.get[Option[String]]("import-offices.phase")
    )

  val defaultLastUpdated: LocalDate =
    LocalDate.parse(config.get[String]("last-updated-date.default"))

  val customsOfficesPageSize = config.get[Int]("customsOfficesPageSize")

  val codeListConfigs: List[CodeListConfig] =
    config
      .get[Seq[Config]]("import-codelists.codelists")
      .toList
      .map { codeListConfig =>
        CodeListConfig(
          CodeListCode.fromString(codeListConfig.getString("code")),
          CodeListOrigin.valueOf(codeListConfig.getString("origin")),
          codeListConfig.getString("keyProperty")
        )
      }

  val domainAndPhaseConfigs: List[DomainAndPhaseJob] =
    config
      .get[Seq[Config]]("domain-and-phase-jobs")
      .toList
      .zipWithIndex
      .map { (domainAndPhaseJobConfig, idx) =>
        val schedule = domainAndPhaseJobConfig.getString("schedule")
        val codelists = domainAndPhaseJobConfig
          .getConfigList("codelists")
          .asScala
          .toList
          .map { codeListConfig =>
            DomainAndPhaseListConfig(CodeListCode.fromString(codeListConfig.getString("code")), CodeListOrigin.valueOf(codeListConfig.getString("origin")), codeListConfig.getString("keyProperty"), domainAndPhaseJobConfig.getString("domain"), domainAndPhaseJobConfig.getString("phase"))
          }
        DomainAndPhaseJob(
          s"domain-and-phase-job-${idx + 1}",
          schedule,
//          domainAndPhaseJobConfig.getString("domain"),
//          domainAndPhaseJobConfig.getString("phase"),
          codelists
        )
      }

//  val phaseAndDomainListConfigs: List[DomainAndPhaseListConfig] =
//    config
//      .get[Seq[Config]]("import-pd-lists.pd-lists")
//      .toList
//      .map { codeListConfig =>
//        DomainAndPhaseListConfig(
//          CodeListCode.fromString(codeListConfig.getString("code")),
//          CodeListOrigin.valueOf(codeListConfig.getString("origin")),
//          codeListConfig.getString("keyProperty"),
//          config.get[String]("import-pd-lists.phase"),
//          config.get[String]("import-pd-lists.domain")
//        ) }

  val correspondenceListConfigs: List[CorrespondenceListConfig] =
    config
      .get[Seq[Config]]("import-correspondence-lists.correspondence-lists")
      .toList
      .map { correspondenceListConfig =>
        CorrespondenceListConfig(
          CodeListCode.fromString(correspondenceListConfig.getString("code")),
          CodeListOrigin.valueOf(correspondenceListConfig.getString("origin")),
          correspondenceListConfig.getString("keyProperty"),
          correspondenceListConfig.getString("valueProperty")
        )
      }

}
