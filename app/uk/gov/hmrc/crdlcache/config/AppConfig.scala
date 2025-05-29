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
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration) extends ServicesConfig(config) {
  val appName: String = config.get[String]("appName")

  val dpsUrl: String          = baseUrl("dps-api")
  val dpsPath: String         = config.get[String]("microservice.services.dps-api.path")
  val dpsClientId: String     = config.get[String]("microservice.services.dps-api.clientId")
  val dpsClientSecret: String = config.get[String]("microservice.services.dps-api.clientSecret")

  val importCodelistsSchedule = config.get[String]("import-codelists.schedule")

  val defaultLastUpdated: LocalDate =
    LocalDate.parse(config.get[String]("import-codelists.last-updated-date.default"))

  val codelistConfigs: List[CodelistConfig] =
    config
      .get[Seq[Config]]("import-codelists.codelists")
      .map { codelistConfig =>
        CodelistConfig(
          CodeListCode.fromString(codelistConfig.getString("code")),
          codelistConfig.getString("keyProperty")
        )
      }
      .toList
}
