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

import org.quartz.{JobDetail, Scheduler}
import org.quartz.spi.TriggerFiredBundle
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.inject.Injector

class ScheduledJobFactorySpec extends AnyFlatSpec with Matchers with MockitoSugar {
  "ScheduledJobFactory" should "instantiate jobs using Play Framework's Guice injector" in {
    val bundle    = mock[TriggerFiredBundle]
    val jobDetail = mock[JobDetail]
    when(bundle.getJobDetail).thenReturn(jobDetail)
    when(jobDetail.getJobClass).thenReturn(classOf[ImportCodeListsJob])

    val injector = mock[Injector]
    val job      = mock[ImportCodeListsJob]
    when(injector.instanceOf(classOf[ImportCodeListsJob])).thenReturn(job)

    val scheduler = mock[Scheduler]
    val factory   = new ScheduledJobFactory(injector)
    val result    = factory.newJob(bundle, scheduler)

    result mustBe job
  }
}
