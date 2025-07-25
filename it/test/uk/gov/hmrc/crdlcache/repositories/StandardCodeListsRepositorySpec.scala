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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{Assertion, OptionValues}
import play.api.libs.json.{JsNull, JsString, JsTrue, Json}
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC36, BC66}
import uk.gov.hmrc.crdlcache.models.CodeListEntry
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.mongo.test.{
  CleanMongoCollectionSupport,
  IndexedMongoQueriesSupport,
  PlayMongoRepositorySupport
}
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration

import java.time.Instant
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class StandardCodeListsRepositorySpec
  extends AnyFlatSpec
  with PlayMongoRepositorySupport[CodeListEntry]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  given TransactionConfiguration = TransactionConfiguration.strict
  given ec: ExecutionContext     = ExecutionContext.global

  override protected val repository: StandardCodeListsRepository = new StandardCodeListsRepository(
    mongoComponent
  )

  override given patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  private val existingCzechEntry = CodeListEntry(
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

  private val activeCodelistEntries = Seq(
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

  private val differentCodeListEntries = Seq(
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

  private val supersededCodeListEntries = Seq(
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

  private val invalidatedIoEntry =
    CodeListEntry(
      BC08,
      "IO",
      "British Indian Ocean Territory",
      Instant.parse("2024-01-31T00:00:00Z"),
      // We don't know exactly when / if IO will cease to be used
      Some(Instant.parse("2026-05-22T00:00:00Z")),
      Some(Instant.parse("2025-05-21T00:00:00Z")),
      Json.obj(
        "actionIdentification" -> "1024"
      )
    )

  private val activeIoEntry = CodeListEntry(
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

  private val postDatedEntry = CodeListEntry(
    BC08,
    "QZ",
    "Disputed Western Territories",
    Instant.parse("2026-06-05T00:00:00Z"),
    None,
    None,
    Json.obj(
      "actionIdentification" -> "9999"
    )
  )

  private val updatedCzechEntry = CodeListEntry(
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

  private val newCountryEntry = CodeListEntry(
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

  def withCodeListEntries(
    entries: Seq[CodeListEntry]
  )(test: ClientSession => Future[Assertion]): Unit = {
    repository.collection.insertMany(entries).toFuture.futureValue
    repository.withSessionAndTransaction(test).futureValue
  }

  private val entriesWithNoEndDate = activeCodelistEntries :+ postDatedEntry

  private val codelistEntries =
    activeCodelistEntries ++ differentCodeListEntries ++ supersededCodeListEntries :+ invalidatedIoEntry :+ postDatedEntry

  "StandardCodeListsRepository.fetchEntryKeys" should "return entries that have not been superseded" in withCodeListEntries(
    codelistEntries
  ) { session =>
    repository.fetchEntryKeys(session, BC08).map(_ must contain("BL"))
  }

  it should "not return entries that are invalidated" in withCodeListEntries(codelistEntries) {
    session =>
      repository.fetchEntryKeys(session, BC08).map(_ mustNot contain("IO"))
  }

  it should "not return entries from other code lists" in withCodeListEntries(codelistEntries) {
    session =>
      repository.fetchEntryKeys(session, BC08).map(_ mustNot contain("B"))
  }

  it should "contain the active code list entry keys" in withCodeListEntries(codelistEntries) {
    session =>
      repository
        .fetchEntryKeys(session, BC08)
        .map {
          _ mustBe entriesWithNoEndDate
            .map(_.key)
            .toSet
        }
  }

  "StandardCodeListsRepository.fetchEntries" should "return the codelist entries whose activeFrom date is before the requested date" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = None,
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain allElementsOf activeCodelistEntries)
  }

  it should "apply filtering of entries according to the supplied keys" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set("AW", "BL")),
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain allElementsOf activeCodelistEntries.take(2))
  }

  it should "not apply filtering of entries when the set of supplied keys is empty" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set.empty),
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain allElementsOf activeCodelistEntries)
  }

  it should "not return entries from other lists" in withCodeListEntries(codelistEntries) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = None,
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain noElementsOf differentCodeListEntries)
  }

  it should "not return entries from other lists even when matching keys are specified" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set("B")),
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain noElementsOf differentCodeListEntries)
  }

  it should "not return entries that have been superseded" in withCodeListEntries(codelistEntries) {
    _ =>
      repository
        .fetchEntries(
          BC08,
          filterKeys = None,
          filterProperties = None,
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )
        .map(_ must contain noElementsOf supersededCodeListEntries)
  }

  it should "not return entries that are not yet active" in withCodeListEntries(codelistEntries) {
    _ =>
      repository
        .fetchEntries(
          BC08,
          filterKeys = None,
          filterProperties = None,
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )
        .map(_ mustNot contain(postDatedEntry))
  }

  it should "return entries that have been invalidated if the invalidation date is in the future" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = None,
        filterProperties = None,
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain(invalidatedIoEntry))
  }

  private val exciseProductEntries = Seq(
    CodeListEntry(
      BC36,
      "B000",
      "Beer",
      Instant.parse("2018-01-16T00:00:00Z"),
      None,
      Some(Instant.parse("2018-01-15T00:00:00Z")),
      Json.obj(
        "exciseProductsCategoryCode"         -> "B",
        "unitOfMeasureCode"                  -> "3",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "degreePlatoApplicabilityFlag"       -> true,
        "densityApplicabilityFlag"           -> false,
        "responsibleDataManager"             -> null
      )
    ),
    CodeListEntry(
      BC36,
      "E200",
      "Vegetable and animal oils Products falling within CN codes 1507 to 1518, if these are intended for use a\ns heating fuel or motor fuel (Article 20(1)(a))",
      Instant.parse("2022-05-15T00:00:00Z"),
      None,
      Some(Instant.parse("2022-05-13T00:00:00Z")),
      Json.obj(
        "exciseProductsCategoryCode"         -> "E",
        "unitOfMeasureCode"                  -> "2",
        "alcoholicStrengthApplicabilityFlag" -> false,
        "degreePlatoApplicabilityFlag"       -> false,
        "densityApplicabilityFlag"           -> true
      )
    ),
    CodeListEntry(
      BC36,
      "W200",
      "Still wine and still fermented beverages other than wine and beer",
      Instant.parse("2023-11-02T00:00:00Z"),
      None,
      Some(Instant.parse("2023-11-01T00:00:00Z")),
      Json.obj(
        "exciseProductsCategoryCode"         -> "W",
        "unitOfMeasureCode"                  -> "3",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "degreePlatoApplicabilityFlag"       -> false,
        "densityApplicabilityFlag"           -> false,
        "responsibleDataManager"             -> "ABC"
      )
    )
  )

  it should "apply filtering of entries using String properties" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC36,
        filterKeys = Some(Set("B000", "W200")),
        filterProperties = Some(Map("exciseProductsCategoryCode" -> JsString("W"))),
        activeAt = Instant.parse("2025-06-05T00:00:00Z")
      )
      .map(_ must contain only exciseProductEntries.last)
  }

  it should "apply filtering of entries using Boolean properties" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    for {
      allEntriesWithFlag <- repository
        .fetchEntries(
          BC36,
          filterKeys = None,
          filterProperties = Some(Map("alcoholicStrengthApplicabilityFlag" -> JsTrue)),
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )

      filteredEntriesWithFlag <- repository
        .fetchEntries(
          BC36,
          filterKeys = Some(Set("B000", "W200")),
          filterProperties = Some(Map("degreePlatoApplicabilityFlag" -> JsTrue)),
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )
    } yield {
      allEntriesWithFlag must contain allOf (exciseProductEntries.head, exciseProductEntries.last)
      filteredEntriesWithFlag must contain only exciseProductEntries.head
    }
  }

  it should "apply filtering of entries using null or missing values" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    for {
      allEntriesWithNull <- repository
        .fetchEntries(
          BC36,
          filterKeys = None,
          filterProperties = Some(Map("responsibleDataManager" -> JsNull)),
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )

      filteredEntriesWithNull <- repository
        .fetchEntries(
          BC36,
          filterKeys = Some(Set("B000", "W200")),
          filterProperties = Some(Map("responsibleDataManager" -> JsNull)),
          activeAt = Instant.parse("2025-06-05T00:00:00Z")
        )
    } yield {
      allEntriesWithNull must contain allElementsOf exciseProductEntries.init
      filteredEntriesWithNull must contain only exciseProductEntries.head
    }
  }

  "StandardCodeListsRepository.executeInstructions" should "invalidate existing entries" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            InvalidateEntry(
              activeIoEntry.copy(activeFrom = Instant.parse("2025-05-22T00:00:00Z"))
            )
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "IO")
          )
        )
        .toFuture()

    } yield entries mustBe Seq(
      activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
    )
  }

  it should "invalidate existing entries when the activation date is the same as the existing record" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            InvalidateEntry(
              activeIoEntry
            )
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "IO")
          )
        )
        .toFuture()

    } yield entries mustBe Seq(activeIoEntry.copy(activeTo = Some(activeIoEntry.activeFrom)))
  }

  it should "invalidate missing entries" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            RecordMissingEntry(BC08, "IO", Instant.parse("2025-05-22T00:00:00Z"))
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "IO")
          )
        )
        .toFuture()

    } yield entries mustBe Seq(
      activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
    )
  }

  it should "invalidate missing entries when the activation date is the same as the existing one" in withCodeListEntries(
    activeCodelistEntries :+ activeIoEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            RecordMissingEntry(BC08, "IO", activeIoEntry.activeFrom)
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "IO")
          )
        )
        .toFuture()

    } yield entries mustBe Seq(activeIoEntry.copy(activeTo = Some(activeIoEntry.activeFrom)))

  }

  it should "supersede and invalidate existing entries" in withCodeListEntries(
    activeCodelistEntries :+ existingCzechEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            UpsertEntry(updatedCzechEntry)
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "CZ")
          )
        )
        .sort(Sorts.ascending("activeFrom"))
        .toFuture()

    } yield entries mustBe Seq(
      existingCzechEntry.copy(activeTo = Some(updatedCzechEntry.activeFrom)),
      updatedCzechEntry
    )
  }

  it should "create a new entry when a new country code is encountered" in withCodeListEntries(
    activeCodelistEntries
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            UpsertEntry(newCountryEntry)
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "SS")
          )
        )
        .toFuture()

    } yield entries mustBe Seq(newCountryEntry)
  }

  it should "replace existing entries with same active from date" in withCodeListEntries(
    activeCodelistEntries :+ existingCzechEntry
  ) { session =>
    for {
      _ <- repository
        .executeInstructions(
          session,
          List(
            UpsertEntry(updatedCzechEntry.copy(activeFrom = existingCzechEntry.activeFrom))
          )
        )

      entries <- repository.collection
        .find(
          session,
          and(
            Filters.eq("codeListCode", BC08.code),
            Filters.eq("key", "CZ")
          )
        )
        .sort(Sorts.ascending("activeFrom"))
        .toFuture()

    } yield entries mustBe Seq(updatedCzechEntry.copy(activeFrom = existingCzechEntry.activeFrom))
  }

  it should "upsert entries in the order of their active from date" in {
    repository.withSessionAndTransaction { session =>
      for {
        _ <- repository
          .executeInstructions(
            session,
            List(
              InvalidateEntry(
                activeIoEntry.copy(activeFrom = Instant.parse("2025-05-22T00:00:00Z"))
              ),
              UpsertEntry(updatedCzechEntry),
              RecordMissingEntry(BC08, "CZ", Instant.parse("2025-01-17T00:00:00Z")),
              UpsertEntry(existingCzechEntry),
              UpsertEntry(activeIoEntry)
            )
          )

        entries <- repository.collection
          .find(
            session,
            and(
              Filters.eq("codeListCode", BC08.code)
            )
          )
          .sort(Sorts.ascending("key", "activeFrom"))
          .toFuture()

      } yield entries mustBe Seq(
        existingCzechEntry.copy(activeTo = Some(updatedCzechEntry.activeFrom)),
        updatedCzechEntry.copy(activeTo = Some(Instant.parse("2025-01-17T00:00:00Z"))),
        activeIoEntry.copy(activeTo = Some(Instant.parse("2025-05-22T00:00:00Z")))
      )
    }.futureValue
  }

}
