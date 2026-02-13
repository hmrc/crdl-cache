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
import uk.gov.hmrc.crdlcache.models.dps.codelist.DpsCodeListEntry
import uk.gov.hmrc.crdlcache.models.dps.col.DpsCustomsOffice

trait TestData {
  val BC08Aruba = Json
    .fromJson[DpsCodeListEntry](
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

  val DK003102 = Json
    .fromJson[DpsCustomsOffice](
      Json.parse(
        """{
      |      "rdentrystatus": {
      |        "state": "valid",
      |        "activefrom": "22-03-2025"
      |      },
      |      "referencenumber": "DK003102",
      |      "referencenumbermainoffice": null,
      |      "referencenumberhigherauthority": null,
      |      "referencenumbercompetentauthorityofenquiry": "DK003102",
      |      "referencenumbercompetentauthorityofrecovery": "DK003102",
      |      "referencenumbertakeover": null,
      |      "countrycode": "DK",
      |      "emailaddress": "test@dk",
      |      "unlocodeid": null,
      |      "nctsentrydate": null,
      |      "nearestoffice": null,
      |      "postalcode": "9850",
      |      "phonenumber": "+45 342234 34543",
      |      "faxnumber": null,
      |      "telexnumber": null,
      |      "geoinfocode": null,
      |      "regioncode": null,
      |      "traderdedicated": "0",
      |      "dedicatedtraderlanguagecode": null,
      |      "dedicatedtradername": null,
      |      "customsofficespecificnotes": [
      |        {
      |          "specificnotescode": "SN0009"
      |        }
      |      ],
      |      "customsofficelsd": [
      |        {
      |          "customsofficeusualname": "Hirtshals Toldekspedition",
      |          "languagecode": "DA",
      |          "city": "Hirtshals",
      |          "prefixsuffixflag": "0",
      |          "prefixsuffixlevel": null,
      |          "spacetoadd": "0",
      |          "streetandnumber": "Dalsagervej 7"
      |        }
      |      ],
      |      "customsofficetimetable": [{
      |        "seasoncode": "1",
      |        "seasonname": null,
      |        "seasonstartdate": "20180101",
      |        "seasonenddate": "20991231",
      |        "customsofficetimetableline": [
      |          {
      |            "dayintheweekbeginday": "1",
      |            "openinghourstimefirstperiodfrom": "0800",
      |            "openinghourstimefirstperiodto": "1600",
      |            "dayintheweekendday": "5",
      |            "openinghourstimesecondperiodfrom": null,
      |            "openinghourstimesecondperiodto": null,
      |            "customsofficeroletrafficcompetence": [
      |              {
      |                "rolename": "EXL",
      |                "traffictype": "P"
      |              },
      |              {
      |                "rolename": "EXL",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "EXP",
      |                "traffictype": "P"
      |              },
      |              {
      |                "rolename": "EXP",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "EXT",
      |                "traffictype": "P"
      |              },
      |              {
      |                "rolename": "EXT",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "PLA",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "RFC",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "DIS",
      |                "traffictype": "N/A"
      |              },
      |              {
      |                "rolename": "IPR",
      |                "traffictype": "N/A"
      |              },
      |              {
      |                "rolename": "ENQ",
      |                "traffictype": "P"
      |              },
      |              {
      |                "rolename": "ENQ",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "ENQ",
      |                "traffictype": "N/A"
      |              },
      |              {
      |                "rolename": "REC",
      |                "traffictype": "P"
      |              },
      |              {
      |                "rolename": "REC",
      |                "traffictype": "R"
      |              },
      |              {
      |                "rolename": "REC",
      |                "traffictype": "N/A"
      |              }
      |            ]
      |          }
      |        ]
      |      }]
      |    }""".stripMargin
      )
    )
    .get
}
