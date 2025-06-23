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

enum CustomsOfficeListsInstruction {
  case UpsertCustomsOffice(customsOffice: CustomsOffice)
  case RecordMissingCustomsOffice(override val referenceNumber: String, removedAt: Instant)

  def referenceNumber: String = this match {
    case CustomsOfficeListsInstruction.UpsertCustomsOffice(customsOffice) =>
      customsOffice.referenceNumber
    case CustomsOfficeListsInstruction.RecordMissingCustomsOffice(referenceNumber, _) =>
      referenceNumber
  }

  def activeFrom: Instant = this match {
    case CustomsOfficeListsInstruction.UpsertCustomsOffice(customsOffice) =>
      customsOffice.activeFrom
    case CustomsOfficeListsInstruction.RecordMissingCustomsOffice(_, removedAt) => removedAt
  }
}
