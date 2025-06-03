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

import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.{Filters, Sorts}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC66}
import uk.gov.hmrc.crdlcache.models.CodeListEntry
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.mongo.test.{
  CleanMongoCollectionSupport,
  IndexedMongoQueriesSupport,
  PlayMongoRepositorySupport
}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

class CodeListsRepositorySpec
  extends AnyFlatSpec
  with PlayMongoRepositorySupport[CodeListEntry]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  given ec: ExecutionContext = ExecutionContext.global

  override protected val repository: CodeListsRepository = new CodeListsRepository(
    mongoComponent
  )

  override given patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  val existingCzechEntry = CodeListEntry(
    BC08,
    "CZ",
    "Czech Republic",
    Instant.parse("2023-01-17T00:00:00Z"),
    None,
    Some(Instant.parse("2023-01-17T00:00:00Z")),
    Json.obj(
      "actionIdentification" -> "850"
    )
  )

  val activeCodelistEntries = Seq(
    CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "811"
      )
    ),
    CodeListEntry(
      BC08,
      "BL",
      "Saint Barthélemy",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "823"
      )
    ),
    CodeListEntry(
      BC08,
      "BM",
      "Bermuda",
      Instant.parse("2024-01-17T00:00:00Z"),
      None,
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "824"
      )
    )
  )

  val differentCodeListEntries = Seq(
    CodeListEntry(
      BC66,
      "B",
      "Beer",
      Instant.parse("2016-10-13T00:00:00Z"),
      None,
      Some(Instant.parse("2016-10-12T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "1084"
      )
    )
  )

  val supersededCodeListEntries = Seq(
    CodeListEntry(
      BC08,
      "BL",
      "Saint Barthélemy",
      Instant.parse("2024-01-15T00:00:00Z"),
      Some(Instant.parse("2024-01-17T00:00:00Z")),
      Some(Instant.parse("2024-01-14T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "1234"
      )
    )
  )

  val invalidatedIoEntry =
    CodeListEntry(
      BC08,
      "IO",
      "British Indian Ocean Territory",
      Instant.parse("2024-01-31T00:00:00Z"),
      Some(Instant.parse("2025-05-22T00:00:00Z")),
      Some(Instant.parse("2025-05-21T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "1024"
      )
    )

  val activeIoEntry = CodeListEntry(
    BC08,
    "IO",
    "British Indian Ocean Territory",
    Instant.parse("2024-01-31T00:00:00Z"),
    None,
    Some(Instant.parse("2024-01-17T00:00:00Z")),
    Json.obj(
      "actionIdentification" -> "1024"
    )
  )

  val updatedCzechEntry = CodeListEntry(
    BC08,
    "CZ",
    "Czechia",
    Instant.parse("2024-01-17T00:00:00Z"),
    None,
    Some(Instant.parse("2024-01-17T00:00:00Z")),
    Json.obj(
      "actionIdentification" -> "850"
    )
  )

  val newCountryEntry = CodeListEntry(
    BC08,
    "SS",
    "South Sudan",
    Instant.parse("2024-01-18T00:00:00Z"),
    None,
    Some(Instant.parse("2024-01-18T00:00:00Z")),
    Json.obj(
      "actionIdentification" -> "1002"
    )
  )

  def withCodeListEntries(entries: Seq[CodeListEntry])(test: => Unit): Unit = {
    repository.collection.insertMany(entries).toFuture.futureValue
    test
  }

  val codelistEntries =
    activeCodelistEntries ++ differentCodeListEntries ++ supersededCodeListEntries :+ invalidatedIoEntry

  "CodeListsRepository.fetchCodeListEntryKeys" should "return entries that have been superseded" in withCodeListEntries(
    codelistEntries
  ) {
    repository.fetchCodeListEntryKeys(BC08).futureValue must contain("BL")
  }

  it should "not return entries that are invalidated" in withCodeListEntries(codelistEntries) {
    repository.fetchCodeListEntryKeys(BC08).futureValue mustNot contain("IO")
  }

  it should "not return entries from other code lists" in withCodeListEntries(codelistEntries) {
    repository.fetchCodeListEntryKeys(BC08).futureValue mustNot contain("B")
  }

  it should "contain the active code list entry keys" in withCodeListEntries(codelistEntries) {
    repository.fetchCodeListEntryKeys(BC08).futureValue mustBe activeCodelistEntries
      .map(_.key)
      .toSet
  }

  "CodeListsRepository.executeInstructions" should "invalidate existing entries" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) {
    repository
      .executeInstructions(
        List(
          InvalidateEntry(
            activeIoEntry.copy(activeFrom = Instant.parse("2025-05-22T00:00:00Z"))
          )
        )
      )
      .futureValue
    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "IO")
        )
      )
      .toFuture()
      .futureValue mustBe Seq(
      activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
    )

  }

  it should "invalidate existing entries when the activation date is the same as the existing record" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) {
    repository
      .executeInstructions(
        List(
          InvalidateEntry(
            activeIoEntry
          )
        )
      )
      .futureValue
    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "IO")
        )
      )
      .toFuture()
      .futureValue mustBe Seq(activeIoEntry.copy(activeTo = Some(activeIoEntry.activeFrom)))

  }

  it should "invalidate missing entries" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) {
    repository
      .executeInstructions(
        List(
          RecordMissingEntry(BC08, "IO", Instant.parse("2025-05-22T00:00:00Z"))
        )
      )
      .futureValue
    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "IO")
        )
      )
      .toFuture()
      .futureValue mustBe Seq(
      activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
    )

  }

  it should "invalidate missing entries when the activation date is the same as the existing one" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) {
    repository
      .executeInstructions(
        List(
          RecordMissingEntry(BC08, "IO", activeIoEntry.activeFrom)
        )
      )
      .futureValue
    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "IO")
        )
      )
      .toFuture()
      .futureValue mustBe Seq(activeIoEntry.copy(activeTo = Some(activeIoEntry.activeFrom)))

  }

  it should "supersede and invalidate existing entries" in withCodeListEntries(
    activeCodelistEntries :+ existingCzechEntry
  ) {
    repository
      .executeInstructions(
        List(
          UpsertEntry(updatedCzechEntry)
        )
      )
      .futureValue

    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "CZ")
        )
      )
      .sort(Sorts.ascending("activeFrom"))
      .toFuture()
      .futureValue mustBe Seq(
      existingCzechEntry.copy(activeTo = Some(updatedCzechEntry.activeFrom)),
      updatedCzechEntry
    )
  }

  it should "create a new entry when a new country code is encountered" in withCodeListEntries(
    activeCodelistEntries
  ) {
    repository
      .executeInstructions(
        List(
          UpsertEntry(newCountryEntry)
        )
      )
      .futureValue

    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "SS")
        )
      )
      .toFuture()
      .futureValue mustBe Seq(newCountryEntry)
  }

  it should "replace existing entries with same active from date" in withCodeListEntries(
    activeCodelistEntries :+ existingCzechEntry
  ) {
    repository
      .executeInstructions(
        List(
          UpsertEntry(updatedCzechEntry.copy(activeFrom = existingCzechEntry.activeFrom))
        )
      )
      .futureValue

    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code),
          Filters.eq("key", "CZ")
        )
      )
      .sort(Sorts.ascending("activeFrom"))
      .toFuture()
      .futureValue mustBe Seq(updatedCzechEntry.copy(activeFrom = existingCzechEntry.activeFrom))
  }

  it should "upsert entries in the order of their active from date" in {
    repository
      .executeInstructions(
        List(
          InvalidateEntry(activeIoEntry.copy(activeFrom = Instant.parse("2025-05-22T00:00:00Z"))),
          UpsertEntry(updatedCzechEntry),
          RecordMissingEntry(BC08, "CZ", Instant.parse("2025-01-17T00:00:00Z")),
          UpsertEntry(existingCzechEntry),
          UpsertEntry(activeIoEntry)
        )
      )
      .futureValue

    repository.collection
      .find(
        and(
          Filters.eq("codeListCode", BC08.code)
        )
      )
      .sort(Sorts.ascending("key", "activeFrom"))
      .toFuture()
      .futureValue mustBe Seq(
      existingCzechEntry.copy(activeTo = Some(updatedCzechEntry.activeFrom)),
      updatedCzechEntry.copy(activeTo = Some(Instant.parse("2025-01-17T00:00:00Z"))),
      activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
    )
  }

}
