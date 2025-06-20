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

import play.api.libs.json.Json
import uk.gov.hmrc.crdlcache.models.dps.codeList.CodeListEntry

trait TestData {
  val BC08Aruba = Json
    .fromJson[CodeListEntry](
      Json.parse(
        """{
        |  "dataitem": [
        |    {
        |      "dataitem_name": "CountryCode",
        |      "dataitem_value": "AW"
        |    },
        |    {
        |      "dataitem_name": "Action_Operation",
        |      "dataitem_value": "U"
        |    },
        |    {
        |      "dataitem_name": "Action_ActivationDate",
        |      "dataitem_value": "17-01-2024"
        |    },
        |    {
        |      "dataitem_name": "Action_ActionIdentification",
        |      "dataitem_value": "811"
        |    },
        |    {
        |      "dataitem_name": "Action_ResponsibleDataManager",
        |      "dataitem_value": null
        |    },
        |    {
        |      "dataitem_name": "Action_ModificationDateAndTime",
        |      "dataitem_value": "17-01-2024"
        |    }
        |  ],
        |  "language": [
        |    {
        |      "lang_code": "en",
        |      "lang_desc": "Aruba"
        |    }
        |  ]
        |}
        |
        |""".stripMargin
      )
    )
    .get
}
