# crdl-cache

This service provides an API and caching layer for transit and excise reference data.

The data originates from the EU CS/RD2 and SEED reference data systems and is hosted at DPS (Data Platform Services).

This service exists to reduce the load on the DPS reference data APIs by caching reference data within MDTP.

## Usage

[API Documentation (1.0)](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml)

### Fetch Codelist Versions

This endpoint is used to fetch the available codelists and their version information.

* **URL**

  `/crdl-cache/lists`

* **Method:**

  `GET`

* **Success Response:**
    * **Status:** 200 <br/>
    * **Content:**
        ```json
        [{
          "codeListCode": "BC08",
          "snapshotVersion": 21,
          "lastUpdated": "2025-06-11T13:47:18.238Z"
        }, {
          "codeListCode": "BC36",
          "snapshotVersion": 9,
          "lastUpdated": "2025-06-11T13:47:10.836Z"
        }]
        ```

* **Sample Request:**

  ```shell
  curl --fail-with-body http://localhost:7252/crdl-cache/lists
  ```

### Fetch Codelist Entries

This endpoint is used to fetch entries of a reference data codelist.

* **URL**

  `/crdl-cache/lists/:code`

* **Method:**

  `GET`

* **Path Parameters**

  **Required:**

    * `code: String` - The codelist code, e.g. `BC08`, `BC36`.

* **Query Parameters**

  **Optional:**

    * `keys: Seq[String]`

      Used to specify the keys of entries to return in the response.

      You can provide comma-separated values for keys, or provide multiple occurrences of the `keys` parameter, or both.

      For example: `?keys=AW,BL&keys=XI&keys=GB`.

    <!-- The `activeAt` parameter is undocumented for now as we await historical data bugfixes at DPS
    * `activeAt: Instant` - The timestamp at which to view entries. If omitted the current timestamp is used.
    -->

    * Any other query parameter name can be used for filtering feed-specific and codelist-specific properties.

      You do not need to prefix the query parameter with `properties`.

      For example: `?unitOfMeasureCode=3&responsibleDataManager=null`.


* **Success Response:**
    * **Status:** 200 <br/>
    * **Content:**
        ```json
        [{
          "key": "BM",
          "value": "Bermuda",
          "properties": {
            "actionIdentification": "824"
          }
        }, {
          "key": "BL",
          "value": "Saint Barthélemy",
          "properties": {
            "actionIdentification": "823"
          }
        }]
        ```

* **Error Response:**

    * **Status:** 400

    * **Description:**

      This error is returned when an invalid codelist code is provided, or when an invalid `activeAt` timestamp is used.

      Unfortunately the [JsonErrorHandler](https://github.com/hmrc/bootstrap-play/blob/466657a13dec046a94ace9d3138dde33bead82e3/bootstrap-backend-play-30/src/main/scala/uk/gov/hmrc/play/bootstrap/backend/http/JsonErrorHandler.scala) of [bootstrap-play](https://github.com/hmrc/bootstrap-play) unconditionally redacts anything which looks like a parameter parsing error.

      We can implement our own error handler if this proves excessively unhelpful to consuming services.

    * **Content:**
      ```json
      {"status": 400, "message": "bad request, cause: REDACTED"}
      ```

* **Sample Request:**

  ```shell
  curl --fail-with-body http://localhost:7252/crdl-cache/lists/BC08 
  ```

### Fetch Customs Office Lists

This endpoint is used to fetch customs office list.

* **URL**

  `/crdl-cache/lists/customs-office`

* **Method:**

  `GET`

* **Path Parameters**

  **Required:**

    * `code: String` - The codelist code, e.g. `BC08`, `BC36`.

* **Query Parameters**

  **Optional:**

    * `activeAt: Instant`

      Used to specify the timestamp at which the office lists are active to return in the response. If omitted the current timestamp is used.

      For example: `?activeAt=2025-06-05T00:00:00Z`.

* **Success Response:**
    * **Status:** 200 <br/>
    * **Content:**
        ```json
        [{
          "dedicatedTraderLanguageCode": "IT",
          "phoneNumber": "0039 0515283611",
          "emailAddress": "uadm.emilia1@adm.gov.it",
          "customsOfficeLsd": {
          "city": "BOLOGNA",
          "prefixSuffixLevel": "A",
          "languageCode": "IT",
          "spaceToAdd": true,
          "customsOfficeUsualName": "EMILIA 1 BOLOGNA",
          "prefixSuffixFlag": false,
          "streetAndNumber": "VIALE PIETRAMELLARA, 1/2"
          },
          "customsOfficeTimetable": {
          "seasonStartDate": "2018-01-01",
          "seasonName": "ALL YEAR",
          "seasonCode": 1,
          "customsOfficeTimetableLine": [
          {
          "dayInTheWeekEndDay": 6,
          "openingHoursTimeFirstPeriodFrom": "0800",
          "dayInTheWeekBeginDay": 1,
          "openingHoursTimeFirstPeriodTo": "1800",
          "customsOfficeRoleTrafficCompetence": [
               {
                 "roleName": "EXC",
                 "trafficType": "R"
               },
               {
                 "roleName": "REG",
                 "trafficType": "N/A"
               }
             ]
           }
          ],
          "seasonEndDate": "2099-12-31"
         },
         "postalCode": "40121",
         "activeFrom": "2025-05-01T00:00:00Z",
         "customsOfficeSpecificNotesCodes": [],
         "traderDedicated": false,
         "nctsEntryDate": "2025-05-01",
         "geoInfoCode": "Q",
         "referenceNumberHigherAuthority": "ITP00002",
         "countryCode": "IT",
         "dedicatedTraderName": "TIN",
         "faxNumber": "0039 0515283622",
         "referenceNumber": "IT223100"
         }]
        ```

* **Error Response:**

    * **Status:** 400

    * **Description:**

      This error is returned when an invalid `activeAt` timestamp is used.

      Unfortunately the [JsonErrorHandler](https://github.com/hmrc/bootstrap-play/blob/466657a13dec046a94ace9d3138dde33bead82e3/bootstrap-backend-play-30/src/main/scala/uk/gov/hmrc/play/bootstrap/backend/http/JsonErrorHandler.scala) of [bootstrap-play](https://github.com/hmrc/bootstrap-play) unconditionally redacts anything which looks like a parameter parsing error.

      We can implement our own error handler if this proves excessively unhelpful to consuming services.

    * **Content:**
      ```json
      {"status": 400, "message": "bad request, cause: REDACTED"}
      ```

* **Sample Request:**

  ```shell
  curl --fail-with-body http://localhost:7252/crdl-cache/lists/customs-office 
  ```

## Development

### Prerequisites

To ensure that you have all the prerequisites for running this service, follow the Developer setup instructions in the MDTP Handbook.

This should ensure that you have the prerequisites for the service installed:

* JDK 21
* sbt 1.10.x or later
* MongoDB 7.x or later
* Service Manager 2.x

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
