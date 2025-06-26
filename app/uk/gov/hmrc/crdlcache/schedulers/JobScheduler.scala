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

package uk.gov.hmrc.crdlcache.schedulers

import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.quartz.{CronScheduleBuilder, Scheduler}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.crdlcache.config.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobScheduler @Inject() (
  lifecycle: ApplicationLifecycle,
  jobFactory: ScheduledJobFactory,
  config: AppConfig
)(using
  ec: ExecutionContext
) extends Logging {
  private val quartz: Scheduler = StdSchedulerFactory.getDefaultScheduler

  // Import code lists
  private val codeListsJobDetail = newJob(classOf[ImportCodeListsJob])
    .withIdentity("import-code-lists")
    .build()

  val customsOfficeListJobDetail = newJob(classOf[ImportCustomsOfficesListJob])
    .withIdentity("import-offices")
    .build()

  private val codeListsJobSchedule = CronScheduleBuilder
    .cronSchedule(config.importCodeListsSchedule)

  val customsOfficeListSchedule = CronScheduleBuilder.cronSchedule(config.importOfficesSchedule)

  private val codeListsJobTrigger = newTrigger()
    .forJob(codeListsJobDetail)
    .withSchedule(codeListsJobSchedule)
    .build()

  val customsOfficesListJob =
    newTrigger()
      .forJob(customsOfficeListJobDetail)
      .withSchedule(customsOfficeListSchedule)
      .build()

  def startCodeListImport(): Unit = {
    quartz.triggerJob(codeListsJobDetail.getKey)
  }

  def startCustomsOfficeListImport(): Unit = {
    quartz.triggerJob(customsOfficeListJobDetail.getKey)
  }

  private def startScheduler(): Unit = {
    val quartz = StdSchedulerFactory.getDefaultScheduler

    quartz.setJobFactory(jobFactory)

    lifecycle.addStopHook(() => Future(quartz.shutdown()))

    quartz.scheduleJob(codeListsJobDetail, codeListsJobTrigger)
    quartz.scheduleJob(customsOfficeListJobDetail, customsOfficesListJob)
    quartz.start()
  }

  startScheduler()
}
