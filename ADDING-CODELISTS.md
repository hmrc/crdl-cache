
## Adding New Code Lists or Correspondence Lists

### Table of Contents

1. [Overview](#Overview)
2. [Example: Adding CL012 LanguageCodes](#example-adding-cl012-languagecodes)

    * [1. Add to CodeListCode.scala](#1-add-to-codelistcodescala)
    * [2. Register the New Code](#2-register-the-new-codelist-in-values)
    * [3. Update application.conf](#3-update-applicationconf)
    * [4. Add Test Coverage](#4-add-test-coverage)
3. [Verifying the Import Works](#verifying-the-import-works)

    * [A. Start The Services](#a-start-the-services)
    * [B. Trigger the Import Job](#b-trigger-the-import-job)
    * [C. Confirm Import Was Successful](#c-confirm-import-was-successful)
4. [Using Bruno](#using-bruno-)

---

### Overview <br/>

A ***Code List*** is a list where each entry is uniquely identified by a `"key"`. Example: Only one entry with `"key": "GB"` in the BC08 (Country) list. <br/>

A ***Correspondence List*** is unique by both `"key"` and `"value"` fields, in other words, these lists can represent a many-to-many relationship between two entities.
in those lists it is valid to have multiple entries for the same "key" as long as they have different "value"s. <br/>


---

### Example: Adding CL012 LanguageCodes <br/>

### 1. Add to `CodeListCode.scala` <br/>

Insert the new case in `models/CodeListCode`: <br/>

```diff
  case BC109 extends CodeListCode("BC109")
+    // CL012 (LanguageCodes)                           
+  case CL012 extends CodeListCode("CL012")
    // CL141 (Customs Offices)                           
  case CL141 extends CodeListCode("CL141")
```

If it’s a **correspondence list**, include the `listType` as the second parameter: <br/>

```diff
+     // E200 (CN Code <-> Excise Products Correspondence)
+   case E200 extends CodeListCode("E200", listType = CORRESPONDENCE)
```

### 2. Register the new codeList in `values` <br/>

Add it to the `values` set in the companion object, keeping logical order: <br/>

```scala
object CodeListCode {
  private val values: Set[CodeListCode] =
+    Set(BC08, BC11, ..., CL012, CL141)
}
```

### 3. Update `application.conf` <br/>

Add configs to the appropriate config block to ensure it gets added to the daily import job:

* For a code list add in `import-codelists`: <br/>

```scala
{
  code = "CL012"
  origin = "CSRD"         // Typically "CL" codes are CSRD; confirm if unsure
  keyProperty = "LanguageCode"  
}
```
> * `BCxxx` codes → `SEED`
> * `CLxxx` codes → `CSRD`


* For a correspondence list add in correspondence-lists: <br/>
   * You will need the key and value properties <br/>

```hocon
{
  code = "E200"
  origin = "SEED"
  keyProperty = "CnCode"
  valueProperty = "ExciseProductCode"
}
```

### 4. Add Test Coverage <br/>

* Update [`AppConfigSpec`](./test/uk/gov/hmrc/crdlcache/config/AppConfigSpec.scala) so it can be included in the tests. <br/>
* Add relevant test data and cases following patterns used for existing lists. <br/>
* Add stub data for the new codeList to [`crdl-ref-data-dps-stub`](https://github.com/hmrc/crdl-ref-data-dps-stub) <br/>

---

## Verifying the Import Works <br/>

### A. Start the services <br/>

   * In terminal <br/>

```shell
  sm2 --start CRDL_REF_DATA_DPS_STUB
```

   * In Intellij terminal <br/>

```shell
  sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes
```
Deleting existing data from the cache to ensure a clean start if needed: <br/>
   * Using test-only endpoints to clear data: <br/>

```shell
  curl -X DELETE http://localhost:7252/crdl-cache/test-only/codelists
  curl -X DELETE http://localhost:7252/crdl-cache/test-only/correspondence-lists
  curl -X DELETE http://localhost:7252/crdl-cache/test-only/last-updated
```

### B. Trigger the Import Job <br/>

```shell
  curl -X POST http://localhost:7252/crdl-cache/test-only/codelists
```


#### Importing data from the DPS HIP API.

1. To test your new codeList can fetch data from DPS and save it to CRDL-CACHE you will need to get the ClientID and ClientSecret from the Integration Hub.
    * Configure your `clientId` and `ClientSecret` in Bash (or however you would normally configure it). <br/>

2. Update `application.conf`:<br/>

   * Uncomment the config below in conf/application.conf - "`# Use for local testing with the real HIP API`" - and comment out the local configs: <br/>
```hocon
microservice {
  services {
    dps-api {
      # host = localhost
      # port = 7253
      # ref-data-path = "crdl-ref-data-dps-stub/iv_crdl_reference_data"
      # customs-offices-path = "crdl-ref-data-dps-stub/iv_crdl_customs_office"
      # clientId = "client_id_must_be_set_in_app-config-xxx"
      # clientSecret = "client_secret_must_be_set_in_app-config-xxx"

      # Use for local testing with the real HIP API:
      protocol = "https"
      host = "admin.qa.tax.service.gov.uk"
      port = 443
      ref-data-path = "hip/crdl/views/iv_crdl_reference_data"
      customs-offices-path = "hip/crdl/views/iv_crdl_customs_office"
      # The following environment variables must be set using credentials from Integration Hub:
      clientId = ${CLIENT_ID}
      clientSecret = ${CLIENT_SECRET}
    }
  }
```
(remember to change it back after testing!)

### C. Confirm Import Was Successful <br/>

Use a curl request in the terminal to fetch the data from CRDL-CACHE: <br/>

```shell
  curl -X GET http://localhost:7252/crdl-cache/lists/CL012
```

---

## Using Bruno <br/>

POST to: <br/>

```shell
  http://localhost:7252/crdl-cache/test-only/codelists
```

---



