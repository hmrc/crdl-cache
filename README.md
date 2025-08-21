# crdl-cache

This service provides an API and caching layer for transit and excise reference data.

The data originates from the EU CS/RD2 and SEED reference data systems and is hosted at DPS (Data Platform Services).

This service exists to reduce the load on the DPS reference data APIs by caching reference data within MDTP.

Please see our [Integration Guide](./INTEGRATION-GUIDE.md) for details of how to integrate your service with the Central Reference Data Library.

## Usage

[API Documentation (1.0)](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml)

### Running the service

1. Make sure you run all the dependant services through the service manager:

```shell
 sm2 --start CRDL_CACHE_ALL
 ```

2. Stop the cache microservice from the service manager and run it locally:

```shell 
sm2 --stop CRDL_CACHE
```

```shell 
sbt run
```
The service runs on port 7252 by default.

You will need to set up a dummy internal-auth token by invoking the test-only token endpoint of **internal-auth**:

```shell
curl -i -X POST -H 'Content-Type: application/json'  -d '{
  "token": "crdl-cache-token",
  "principal": "emcs-tfe-crdl-reference-data",
  "permissions": [{
    "resourceType": "crdl-cache",
    "resourceLocation": "*",
    "actions": ["READ"]
  }]
}' 'http://localhost:8470/test-only/token'
```

And now you can use curl to fetch the data from the **crdl-cache**, For example:

```shell
curl -H 'Authorization: crdl-cache-token' http://localhost:7252/crdl-cache/lists/BC08
```
### Sample Bruno Requests
To request data from the crdl-cache using Bruno, open the [CRDL-CACHE(LOCAL)](./bruno/CRDL-CACHE(LOCAL)) collection on your bruno application, the file that can be found in the .bruno folder.

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
  curl -H 'Authorization: crdl-cache-token' --fail-with-body http://localhost:7252/crdl-cache/lists
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

      This error is returned when an invalid codelist code is provided.

      Unfortunately the [JsonErrorHandler](https://github.com/hmrc/bootstrap-play/blob/466657a13dec046a94ace9d3138dde33bead82e3/bootstrap-backend-play-30/src/main/scala/uk/gov/hmrc/play/bootstrap/backend/http/JsonErrorHandler.scala) of [bootstrap-play](https://github.com/hmrc/bootstrap-play) unconditionally redacts anything which looks like a parameter parsing error.

      We can implement our own error handler if this proves excessively unhelpful to consuming services.

    * **Content:**
      ```json
      {"status": 400, "message": "bad request, cause: REDACTED"}
      ```

* **Sample Request:**

  ```shell
  curl -H 'Authorization: crdl-cache-token' --fail-with-body http://localhost:7252/crdl-cache/lists/BC08 
  ```

### Fetch Customs Office Lists

This endpoint is used to fetch customs office list.

* **URL**

  `/crdl-cache/offices`

* **Method:**

  `GET`

  * **Query Parameters**

    **Optional:**

      * `referenceNumbers: Seq[String]`

        Used to specify the reference numbers of the offices to return in the response.
      
        You can provide comma-separated values for reference numbers, or provide multiple occurrences of the referenceNumbers parameter, or both.

        For example: `?referenceNumbers=IT314102,DK314102&referenceNumbers=DE005055&referenceNumbers=GB005055`.
    
      * `countryCodes: Seq[String]`

        Used to specify the country codes of the offices to return in the response.

        You can provide comma-separated values for country codes, or provide multiple occurrences of the countryCodes parameter, or both.
    
        For example: `?countryCodes=CZ,SK&countryCodes=XI&countryCodes=GB`.
    
      * `roles: Seq[String]`

        Used to specify the roles of the offices to return in the response.

        You can provide comma-separated values for roles, or provide multiple occurrences of the roles parameter, or both.

        For example: `?roles=ACE,RSS&roles=AUT&roles=CCA`.
         <!-- The `activeAt` parameter is undocumented for now as we await historical data bugfixes at DPS
         * `activeAt: Instant` - The timestamp at which to view entries. If omitted the current timestamp is used.
         -->
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

* **Sample Request:**

  ```shell
  curl -H 'Authorization: crdl-cache-token' --fail-with-body http://localhost:7252/crdl-cache/lists/offices 
  ```


## Development

### Adding a new code list or correspondence list code to the import job
The steps for doing so have been explained in detail on here [ADDING-CODELISTS.md](./ADDING-CODELISTS.md)

### Importing data from DPS API

When the code list, correspondence list or customs office list are imported via the scheduled jobs or via test-only endpoints as descripted in [ADDING-CODELISTS.md](./ADDING-CODELISTS.md#verifying-the-import-works) they are by default imported from our stubs.
In order to import the actual data from the DPS API, in our [application.conf](./conf/application.conf) we need to comment out the config which calls our stub and uncomment the config which calls the DPS API. The resulting config would look like:
```diff
microservice {
  services {
    dps-api {
-      host = localhost
-      port = 7253
-      ref-data-path = "crdl-ref-data-dps-stub/iv_crdl_reference_data"
-      customs-offices-path = "crdl-ref-data-dps-stub/iv_crdl_customs_office"
-      clientId = "client_id_must_be_set_in_app-config-xxx"
-      clientSecret = "client_secret_must_be_set_in_app-config-xxx"
+      # host = localhost
+      # port = 7253
+      # ref-data-path = "crdl-ref-data-dps-stub/iv_crdl_reference_data"
+      # customs-offices-path = "crdl-ref-data-dps-stub/iv_crdl_customs_office"
+      # clientId = "client_id_must_be_set_in_app-config-xxx"
+      # clientSecret = "client_secret_must_be_set_in_app-config-xxx"

-      # # Use for local testing with the real HIP API:
-      # protocol = "https"
-      # host = "admin.qa.tax.service.gov.uk"
-      # port = 443
-      # ref-data-path = "hip/crdl/views/iv_crdl_reference_data"
-      # customs-offices-path = "hip/crdl/views/iv_crdl_customs_office"
-      # # The following environment variables must be set using credentials from Integration Hub:
-      # clientId = ${CLIENT_ID}
-      # clientSecret = ${CLIENT_SECRET}
+      # Use for local testing with the real HIP API:
+      protocol = "https"
+      host = "admin.qa.tax.service.gov.uk"
+      port = 443
+      ref-data-path = "hip/crdl/views/iv_crdl_reference_data"
+      customs-offices-path = "hip/crdl/views/iv_crdl_customs_office"
+      # The following environment variables must be set using credentials from Integration Hub:
+      clientId = ${CLIENT_ID}
+      clientSecret = ${CLIENT_SECRET}
    }
  }
```

Please Note: To obtain the client id and secret, you can go to the [Integration Hub](https://admin.tax.service.gov.uk/integration-hub) and login with your LDAP credentials. Click on the 'Central Reference Data Cache' registered application and navigate to Test > Credentails.
Follow the below-mentioned steps to set up the client id and secret as environment variables. Post that when the import job is triggered next it would fetch the data from the DPS API.

### Saving CLIENT_ID and CLIENT_SECRET as environment variables
Open your shell configuration file(It can be .bashrc or .zshrc or any other shell) in an editor of your choice.

Add these lines to the file. Replace <your-client-id> with your client id and <your-client-secret> with your client secret.
 ```shell
    export CLIENT_ID=<your-client-id>
    export CLIENT_SECRET=<your-client-secret>
 ```
Save and close the file.

Reload your shell configuration by executing
 ```shell
  source .bashrc #or source .zshrc depending on your shell
  ```
Verify the variables
 ```shell
    echo $CLIENT_ID
    echo $CLIENT_SECRET
 ```

### Prerequisites

To ensure that you have all the prerequisites for running this service, follow the Developer setup instructions in the MDTP Handbook.

This should ensure that you have the prerequisites for the service installed:

* JDK 21
* sbt 1.10.x or later
* MongoDB 7.x or later
* Service Manager 2.x

### All tests and checks
This is an sbt command alias specific to this project. It will run a scala format
check, run unit tests, run integration tests and produce a coverage report:
> `sbt runAllChecks`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
