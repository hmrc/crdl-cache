# crdl-cache

This service provides an API and caching layer for transit and excise reference data.

The data originates from the EU CS/RD2 and SEED reference data systems and is hosted at DPS (Data Platform Services).

This service exists to reduce the load on the DPS reference data APIs by caching reference data within MDTP.

## Usage

[API Documentation (1.0)](https://redocly.github.io/redoc/?url=https%3A%2F%2Fcdn.jsdelivr.net%2Fgh%2Fhmrc%2Fcrdl-cache%40main%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml)

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

    * `activeAt: Instant` - The timestamp at which to view entries. If omitted the current timestamp is used.

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
