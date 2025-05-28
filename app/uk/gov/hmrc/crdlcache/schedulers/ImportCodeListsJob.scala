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

import org.quartz.{Job, JobExecutionContext}
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.Inject
import scala.concurrent.duration.*

class ImportCodeListsJob @Inject() (val lockRepository: MongoLockRepository)
  extends Job
  with LockService {

  def execute(context: JobExecutionContext): Unit = () // TODO: Job

  val lockId: String = "import-code-lists"
  val ttl: Duration  = 1.hour
}
