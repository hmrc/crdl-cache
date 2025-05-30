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

package uk.gov.hmrc.crdlcache.repositories

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.crdlcache.models.LastUpdated
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.{Clock, ZoneOffset}
import scala.concurrent.ExecutionContext

class LastUpdatedRepositorySpec
  extends AnyFlatSpec
  with PlayMongoRepositorySupport[LastUpdated]
  with CleanMongoCollectionSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  private val clock = Clock.tickMillis(ZoneOffset.UTC)

  given ec: ExecutionContext = ExecutionContext.global

  override protected val repository: LastUpdatedRepository = new LastUpdatedRepository(
    mongoComponent
  )

  "LastUpdatedRepository.fetchLastUpdated" should "return None when no last updated date has been populated" in {
    repository.fetchLastUpdated().futureValue mustBe None
  }

  "LastUpdatedRepository.setLastUpdated" should "upsert the last updated date when it is not present" in {
    val inputInstant = clock.instant()
    whenReady(repository.setLastUpdated(inputInstant)) { _ =>
      repository.fetchLastUpdated().futureValue mustBe Some(inputInstant)
    }
  }

  it should "return the latest value when updated multiple times" in {
    val instant1 = clock.instant()
    val instant2 = instant1.plusSeconds(20)
    whenReady(repository.setLastUpdated(instant1)) { _ =>
      whenReady(repository.setLastUpdated(instant2)) { _ =>
        repository.fetchLastUpdated().futureValue mustBe Some(instant2)
      }
    }
  }
}
