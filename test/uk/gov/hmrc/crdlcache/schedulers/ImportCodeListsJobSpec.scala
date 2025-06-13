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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.config.{AppConfig, CodeListConfig}
import uk.gov.hmrc.crdlcache.connectors.DpsConnector
import uk.gov.hmrc.crdlcache.models.CodeListCode.{BC08, BC66}
import uk.gov.hmrc.crdlcache.models.CodeListOrigin.SEED
import uk.gov.hmrc.crdlcache.models.Instruction.{InvalidateEntry, RecordMissingEntry, UpsertEntry}
import uk.gov.hmrc.crdlcache.models.dps.RelationType.{Next, Prev, Self}
import uk.gov.hmrc.crdlcache.models.dps.{Relation, codeList}
import uk.gov.hmrc.crdlcache.models.dps.codeList.{CodeListResponse, DataItem, LanguageDescription}
import uk.gov.hmrc.crdlcache.models.{CodeListEntry, CodeListSnapshot, Instruction, dps}
import uk.gov.hmrc.crdlcache.repositories.{CodeListsRepository, LastUpdatedRepository}
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}

import java.time.{Clock, Instant, LocalDate, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class ImportCodeListsJobSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterEach {
  private val lockRepository        = mock[MongoLockRepository]
  private val lastUpdatedRepository = mock[LastUpdatedRepository]
  private val codeListsRepository   = mock[CodeListsRepository]
  private val dpsConnector          = mock[DpsConnector]
  private val appConfig             = mock[AppConfig]
  private val fixedInstant          = Instant.parse("2025-06-03T00:00:00Z")
  private val clock                 = Clock.fixed(fixedInstant, ZoneOffset.UTC)

  given ActorSystem      = ActorSystem("test")
  given ExecutionContext = ExecutionContext.global

  private val codeListsJob = new ImportCodeListsJob(
    lockRepository,
    lastUpdatedRepository,
    codeListsRepository,
    dpsConnector,
    appConfig,
    clock
  )

  private val snapshotsPage1 = CodeListResponse(
    List(
      codeList.CodeListSnapshot(
        BC08,
        "Country",
        1,
        List(
          codeList.CodeListEntry(
            List(
              DataItem("CountryCode", Some("BL")),
              codeList.DataItem("Action_Operation", Some("U")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("823")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(LanguageDescription("en", "Saint Barthélemy"))
          ),
          codeList.CodeListEntry(
            List(
              codeList.DataItem("CountryCode", Some("BM")),
              codeList.DataItem("Action_Operation", Some("C")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("824")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(codeList.LanguageDescription("en", "Bermuda"))
          )
        )
      )
    ),
    List(
      Relation(
        Self,
        "https://localhost:9443/server/central_reference_data_library/ws_iv_crdl_reference_data/views/iv_crdl_reference_data?%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      ),
      dps.Relation(
        Next,
        "?%24start_index=10&%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      )
    )
  )

  private val snapshotsPage2 = codeList.CodeListResponse(
    List(
      codeList.CodeListSnapshot(
        BC08,
        "Country",
        2,
        List(
          codeList.CodeListEntry(
            List(
              codeList.DataItem("CountryCode", Some("AD")),
              codeList.DataItem("Action_Operation", Some("C")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("1027")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(codeList.LanguageDescription("en", "Andorra"))
          ),
          codeList.CodeListEntry(
            List(
              codeList.DataItem("CountryCode", Some("BL")),
              codeList.DataItem("Action_Operation", Some("I")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("823")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(codeList.LanguageDescription("en", "Saint Barthélemy"))
          ),
          codeList.CodeListEntry(
            List(
              codeList.DataItem("CountryCode", Some("CX")),
              codeList.DataItem("Action_Operation", Some("C")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("848")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(codeList.LanguageDescription("en", "Christmas Island"))
          ),
          codeList.CodeListEntry(
            List(
              codeList.DataItem("CountryCode", Some("CY")),
              codeList.DataItem("Action_Operation", Some("C")),
              codeList.DataItem("Action_ActivationDate", Some("17-01-2024")),
              codeList.DataItem("Action_ActionIdentification", Some("849")),
              codeList.DataItem("Action_ResponsibleDataManager", None),
              codeList.DataItem("Action_ModificationDateAndTime", Some("17-01-2024"))
            ),
            List(codeList.LanguageDescription("en", "Cyprus"))
          )
        )
      )
    ),
    List(
      dps.Relation(
        Self,
        "https://localhost:9443/server/central_reference_data_library/ws_iv_crdl_reference_data/views/iv_crdl_reference_data?%24orderby=snapshotversion+ASC&code_list_code=BC08&%24start_index=10&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      ),
      dps.Relation(
        Prev,
        "?%24orderby=snapshotversion+ASC&code_list_code=BC08&last_updated_date=2025-05-28T00%3A00%3A00Z&%24count=10"
      )
    )
  )

  override def beforeEach(): Unit = {
    reset(lockRepository, lastUpdatedRepository, codeListsRepository, dpsConnector, appConfig)

    // Job lock
    val mockLock = mock[Lock]
    when(lockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(mockLock)))
    when(lockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

    // Instruction execution
    when(codeListsRepository.executeInstructions(any[List[Instruction]])).thenReturn(Future.unit)
  }

  "ImportCodeListsJob.importCodeLists" should "import the configured codelists when there is no last updated date stored" in {
    val lastUpdated        = LocalDate.of(2025, 3, 12)
    val lastUpdatedInstant = lastUpdated.atStartOfDay(ZoneOffset.UTC).toInstant

    // Last updated date
    when(appConfig.defaultLastUpdated).thenReturn(lastUpdated)
    when(lastUpdatedRepository.fetchLastUpdated()).thenReturn(Future.successful(None))
    when(lastUpdatedRepository.setLastUpdated(equalTo(fixedInstant))).thenReturn(Future.unit)

    // Codelist manipulation
    when(codeListsRepository.fetchCodeListEntryKeys(equalTo(BC08)))
      .thenReturn(Future.successful(Set.empty[String]))
      .thenReturn(Future.successful(Set("BL", "BM")))

    // Codelist configuration
    when(appConfig.codeListConfigs).thenReturn(
      List(
        CodeListConfig(BC08, SEED, "CountryCode"),
        CodeListConfig(BC66, SEED, "ExciseProductsCategoryCode")
      )
    )

    // DPS connector responses
    when(
      dpsConnector.fetchCodeListSnapshots(equalTo(BC08), equalTo(lastUpdatedInstant))(using any())
    )
      .thenReturn(Source(List(snapshotsPage1, snapshotsPage2)))

    when(
      dpsConnector.fetchCodeListSnapshots(equalTo(BC66), equalTo(lastUpdatedInstant))(using any())
    )
      .thenReturn(Source.empty)

    codeListsJob.importCodeLists().futureValue

    verify(codeListsRepository, times(1)).executeInstructions(
      equalTo(
        List(
          UpsertEntry(
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
            )
          ),
          UpsertEntry(
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
        )
      )
    )

    verify(codeListsRepository, times(2)).executeInstructions(any())
    verify(lastUpdatedRepository, times(1)).setLastUpdated(fixedInstant)
  }

  it should "import the configured codelists when there is a last updated date stored" in {
    // Last updated date
    when(appConfig.defaultLastUpdated).thenReturn(LocalDate.of(2025, 3, 12))
    when(lastUpdatedRepository.fetchLastUpdated()).thenReturn(Future.successful(Some(fixedInstant)))
    when(lastUpdatedRepository.setLastUpdated(equalTo(fixedInstant))).thenReturn(Future.unit)

    // Codelist manipulation
    when(codeListsRepository.fetchCodeListEntryKeys(equalTo(BC08)))
      .thenReturn(Future.successful(Set.empty[String]))
      .thenReturn(Future.successful(Set("BL", "BM")))

    // Codelist configuration
    when(appConfig.codeListConfigs).thenReturn(
      List(
        CodeListConfig(BC08, SEED, "CountryCode"),
        CodeListConfig(BC66, SEED, "ExciseProductsCategoryCode")
      )
    )

    // DPS connector responses
    when(
      dpsConnector.fetchCodeListSnapshots(equalTo(BC08), equalTo(fixedInstant))(using any())
    )
      .thenReturn(Source(List(snapshotsPage1, snapshotsPage2)))

    when(
      dpsConnector.fetchCodeListSnapshots(equalTo(BC66), equalTo(fixedInstant))(using any())
    )
      .thenReturn(Source.empty)

    codeListsJob.importCodeLists().futureValue

    verify(codeListsRepository, times(2)).executeInstructions(any())
    verify(lastUpdatedRepository, times(1)).setLastUpdated(fixedInstant)
  }

  "ImportCodeListsJob.processSnapshot" should "produce a list of instructions for a snapshot that contains only new entries" in {
    when(codeListsRepository.fetchCodeListEntryKeys(equalTo(BC08)))
      .thenReturn(Future.successful(Set.empty[String]))

    val codeListConfig = CodeListConfig(BC08, SEED, "CountryCode")

    val instructions = codeListsJob
      .processSnapshot(
        codeListConfig,
        CodeListSnapshot.fromDpsSnapshot(codeListConfig, snapshotsPage1.elements.head)
      )
      .futureValue

    instructions.sortBy(ins => (ins.key, ins.activeFrom)) mustBe List(
      UpsertEntry(
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
        )
      ),
      UpsertEntry(
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
    )

  }

  it should "produce a list of instructions for a snapshot which contains invalidations and missing entries" in {
    when(codeListsRepository.fetchCodeListEntryKeys(equalTo(BC08)))
      .thenReturn(Future.successful(Set("BL", "BM")))

    val codeListConfig = CodeListConfig(BC08, SEED, "CountryCode")

    val instructions = codeListsJob
      .processSnapshot(
        codeListConfig,
        CodeListSnapshot.fromDpsSnapshot(codeListConfig, snapshotsPage2.elements.head)
      )
      .futureValue

    instructions.sortBy(ins => (ins.key, ins.activeFrom)) mustBe List(
      UpsertEntry(
        CodeListEntry(
          BC08,
          "AD",
          "Andorra",
          Instant.parse("2024-01-17T00:00:00Z"),
          None,
          Some(Instant.parse("2024-01-17T00:00:00Z")),
          Json.obj(
            "actionIdentification" -> "1027"
          )
        )
      ),
      InvalidateEntry(
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
        )
      ),
      RecordMissingEntry(BC08, "BM", fixedInstant),
      UpsertEntry(
        CodeListEntry(
          BC08,
          "CX",
          "Christmas Island",
          Instant.parse("2024-01-17T00:00:00Z"),
          None,
          Some(Instant.parse("2024-01-17T00:00:00Z")),
          Json.obj(
            "actionIdentification" -> "848"
          )
        )
      ),
      UpsertEntry(
        CodeListEntry(
          BC08,
          "CY",
          "Cyprus",
          Instant.parse("2024-01-17T00:00:00Z"),
          None,
          Some(Instant.parse("2024-01-17T00:00:00Z")),
          Json.obj(
            "actionIdentification" -> "849"
          )
        )
      )
    )
  }
}
