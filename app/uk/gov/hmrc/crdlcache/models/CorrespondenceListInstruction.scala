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

package uk.gov.hmrc.crdlcache.models

import java.time.Instant

enum CorrespondenceListInstruction {
  case UpsertEntry(codeListEntry: CodeListEntry)

  case InvalidateEntry(codeListEntry: CodeListEntry)

  // case DeleteEntry(codeListEntry: CodeListEntry)

  case RecordMissingEntry(
    codeListCode: CodeListCode,
    override val key: String,
    override val value: String,
    removedAt: Instant
  )

  def key: String = this match {
    case CorrespondenceListInstruction.UpsertEntry(codeListEntry)     => codeListEntry.key
    case CorrespondenceListInstruction.InvalidateEntry(codeListEntry) => codeListEntry.key
    // case CorrespondenceListInstruction.DeleteEntry(codeListEntry)     => codeListEntry.key
    case CorrespondenceListInstruction.RecordMissingEntry(_, key, _, _) => key
  }

  def value: String = this match {
    case CorrespondenceListInstruction.UpsertEntry(codeListEntry)     => codeListEntry.value
    case CorrespondenceListInstruction.InvalidateEntry(codeListEntry) => codeListEntry.value
    // case Instruction.DeleteEntry(codeListEntry)     => codeListEntry.key
    case CorrespondenceListInstruction.RecordMissingEntry(_, _, value, _) => value
  }

  def activeFrom: Instant = this match {
    case CorrespondenceListInstruction.UpsertEntry(codeListEntry)     => codeListEntry.activeFrom
    case CorrespondenceListInstruction.InvalidateEntry(codeListEntry) => codeListEntry.activeFrom
    // case CorrespondenceListInstruction.DeleteEntry(codeListEntry)     => codeListEntry.activeFrom
    case CorrespondenceListInstruction.RecordMissingEntry(_, _, _, removedAt) => removedAt
  }
}
