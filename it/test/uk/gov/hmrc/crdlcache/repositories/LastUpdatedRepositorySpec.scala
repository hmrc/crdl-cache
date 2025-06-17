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

import org.mongodb.scala.ClientSession
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{Assertion, OptionValues}
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC66}
import uk.gov.hmrc.crdlcache.models.LastUpdated
import uk.gov.hmrc.mongo.test.{
  CleanMongoCollectionSupport,
  IndexedMongoQueriesSupport,
  PlayMongoRepositorySupport
}
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration

import java.time.{Clock, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class LastUpdatedRepositorySpec
  extends AnyFlatSpec
  with PlayMongoRepositorySupport[LastUpdated]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  private val clock = Clock.tickMillis(ZoneOffset.UTC)

  given TransactionConfiguration = TransactionConfiguration.strict
  given ExecutionContext         = ExecutionContext.global

  override protected val repository: LastUpdatedRepository = new LastUpdatedRepository(
    mongoComponent
  )

  private def withSession(test: ClientSession => Future[Assertion]): Unit =
    repository.withClientSession(test).futureValue

  "LastUpdatedRepository.fetchLastUpdated" should "return None when no last updated date has been populated" in {
    repository.fetchLastUpdated(BC08).futureValue mustBe None
  }

  "LastUpdatedRepository.setLastUpdated" should "upsert the last updated date when it is not present" in withSession {
    session =>
      val inputInstant = clock.instant()
      for {
        _           <- repository.setLastUpdated(session, BC08, 1, inputInstant)
        lastUpdated <- repository.fetchLastUpdated(BC08)
      } yield lastUpdated mustBe Some(LastUpdated(BC08, 1, inputInstant))
  }

  it should "only upsert the value for the specified codelist" in withSession { session =>
    val inputInstant = clock.instant()
    for {
      _           <- repository.setLastUpdated(session, BC08, 1, inputInstant)
      lastUpdated <- repository.fetchLastUpdated(BC66)
    } yield lastUpdated mustBe None
  }

  it should "return the latest value when updated multiple times" in withSession { session =>
    val instant1 = clock.instant()
    val instant2 = instant1.plusSeconds(20)
    for {
      _           <- repository.setLastUpdated(session, BC08, 1, instant1)
      _           <- repository.setLastUpdated(session, BC08, 2, instant2)
      lastUpdated <- repository.fetchLastUpdated(BC08)
    } yield lastUpdated mustBe Some(LastUpdated(BC08, 2, instant2))
  }

  it should "return the latest value only for the specified codelist" in withSession { session =>
    val instant1 = clock.instant()
    val instant2 = instant1.plusSeconds(20)
    for {
      _            <- repository.setLastUpdated(session, BC08, 1, instant1)
      _            <- repository.setLastUpdated(session, BC66, 1, instant2)
      lastUpdated1 <- repository.fetchLastUpdated(BC08)
      lastUpdated2 <- repository.fetchLastUpdated(BC66)
    } yield {
      lastUpdated1 mustBe Some(LastUpdated(BC08, 1, instant1))
      lastUpdated2 mustBe Some(LastUpdated(BC66, 1, instant2))
    }
  }

  it should "return the latest last updated value only for all the codelists" in withSession {
    session =>
      val instant1 = clock.instant()
      val instant2 = instant1.plusSeconds(20)
      for {
        _           <- repository.setLastUpdated(session, BC08, 1, instant1)
        _           <- repository.setLastUpdated(session, BC08, 2, instant2)
        _           <- repository.setLastUpdated(session, BC66, 1, instant2)
        lastUpdated <- repository.fetchAllLastUpdated
      } yield {
        lastUpdated mustBe List(LastUpdated(BC08, 2, instant2), LastUpdated(BC66, 1, instant2))
      }
  }
}
