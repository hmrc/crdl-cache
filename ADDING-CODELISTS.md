# Adding New Code Lists or Correspondence Lists

## Table of Contents

1. [Overview](#overview)
2. [Example: Adding CL012 (Language Codes)](#example-adding-cl012-language-codes)
    * [1. Add to CodeListCode.scala](#1-add-to-codelistcodescala)
    * [2. Register the New Codelist](#2-register-the-new-codelist-in-the-values-set)
    * [3. Update application.conf](#3-update-applicationconf)
    * [4. Add Test Coverage](#4-add-test-coverage)
    * [5. Add the stub data](#5-add-stub-data)
3. [Verifying the Import Works](#verifying-the-import-works)
    * [A. Start The Services](#a-start-the-services)
    * [B. Trigger the Appropriate Import Job](#b-trigger-the-appropriate-import-job)
    * [C. Confirm the Import Was Successful](#c-confirm-the-import-was-successful)

---

## Overview

A ***Code List*** is a list where each entry is uniquely identified by its `key`.

For example: There can be only one entry with `key` of `"GB"` in the BC08 (Country) list. This makes sense because this list identifies unique countries. There can't be two identical countries!

A ***Correspondence List*** is unique by both `key` and `value` fields, in other words, these lists can represent a many-to-many relationship between two entities.
In those lists it is valid to have multiple entries for the same `key` as long as they have different `value`s.

For example, the E200 list represents the mapping between excise product codes and CN codes. It is valid for a CN code to be mapped to multiple excise products, and likewise many CN codes can be mapped to any given excise product code.

---

## Example: Adding CL012 (Language Codes)

### 1. Add to CodeListCode.scala

Insert the new case in `models/CodeListCode`:

```diff
  case BC109 extends CodeListCode("BC109")
+  // CL012 (Language Codes)                           
+  case CL012 extends CodeListCode("CL012")
  // CL141 (Customs Offices)                           
  case CL141 extends CodeListCode("CL141")
```

If it’s a **correspondence list**, include the `listType` as the second parameter. As CL012 is not an example of a correspondence list, we will use the existing E200 code as an example here:

```scala
   // E200 (CN Code <-> Excise Products Correspondence)
   case E200 extends CodeListCode("E200", listType = CORRESPONDENCE)
```

### 2. Register the new codelist in the values set

Add it to the `values` set in the companion object. We have tried to maintain lexical order of entries, but this is not necessary.

```scala
object CodeListCode {
  private val values: Set[CodeListCode] =
    Set(BC08, BC11, ..., CL012, CL141)
}
```

### 3. Update application.conf

Add a new codelist configuration to the appropriate config block to ensure it gets added to the daily import job.

* For a code list, add it to `import-codelists.codelists`.

  You will need a `keyProperty` which determines the data item in the DPS API response to use as the `key` of the entry. For example, in the NCTS entity definitions contain an `isprimarykey` property which can help to identify the `keyProperty`.
  
  You will also need to know the `origin` of the list. This indicates which feed the list is from:

  `BCxxx` codes come from the `SEED` feed (System for Exchange of Excise Data). This is the reference data feed for the European excise system used by services like EMCS.
  
  `CLxxx` codes come from the `CSRD` feed (Central Services Reference Data). This is the reference data feed for the European customs and transit system used by services like NCTS.

  We need to know this because the underlying DPS API does not normalise the feed structure, so each feed has its own feed-specific properties.

  ```hocon
  {
    code = "CL012"
    origin = "CSRD"
    keyProperty = "LanguageCode"  
  }
  ```

* For a correspondence list, add it to `import-correspondence-lists.correspondence-lists`.

  You will need a `keyProperty` and `valueProperty` which determine the data items in the DPS API response to use as the `key` and `value` of the entry. The `valueProperty` would be helpful when there is composite key in the entity definition, for example, if we had two `isprimarykey` fields.

  ```hocon
  {
    code = "E200"
    origin = "SEED"
    keyProperty = "CnCode"
    valueProperty = "ExciseProductCode"
  }
  ```

### 4. Add Test Coverage

* Update [AppConfigSpec](./test/uk/gov/hmrc/crdlcache/config/AppConfigSpec.scala) to include the new default config from [application.conf](./conf/application.conf).
* You can use the existing configuration and test cases as examples.

### 5. Add Stub Data

The steps to add the stub data to the [crdl-ref-data-dps-stub](https://github.com/hmrc/crdl-ref-data-dps-stub) have been detailed on here [Adding stub data](https://github.com/hmrc/crdl-ref-data-dps-stub/blob/main/README.md#adding-stub-data)

---

## Verifying the Import Works

### A. Start the services

You can start the dependencies of the service using service manager:

```shell
sm2 --start CRDL_CACHE_ALL
```

You can start the service itself using sbt. You will need the test-only routes enabled to trigger the import jobs:

```shell
sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes
```

Delete existing data from the cache to ensure a clean start if needed.

This can be done using the test-only endpoints of the service to clear the data:

```shell
curl -X DELETE http://localhost:7252/crdl-cache/test-only/codelists
curl -X DELETE http://localhost:7252/crdl-cache/test-only/correspondence-lists
curl -X DELETE http://localhost:7252/crdl-cache/test-only/last-updated
```

### B. Trigger The Appropriate Import Job

For codelists:
```shell
curl -X POST http://localhost:7252/crdl-cache/test-only/codelists
```

For correspondence lists:
```shell
curl -X POST http://localhost:7252/crdl-cache/test-only/correspondence-lists
```

#### Importing data from the DPS HIP API

The API used by this service to fetch its data is the [SEED and CSRD Reference Data](https://admin.tax.service.gov.uk/integration-hub/apis/view-specification/38a94f8f-e17f-41c3-863a-bc7b4a37c93d) API.

For local development and the staging environment, we provide a stub [crdl-ref-data-dps-stub](https://github.com/hmrc/crdl-ref-data-dps-stub), but you will want to check that the new service can be imported successfully using the real HIP API.

In order to do this you need some credentials to call the HIP API. You can fetch these credentials by logging in to the [Integration Hub](https://admin.tax.service.gov.uk/integration-hub).

Hopefully, if you are making changes to the application, your team owns the "Central Reference Data Cache" application in the Integration Hub.

You can find credentials for Test environment by viewing the application details of the Central Reference Data Cache application. The credentials can be found on the Credentials tab of the Test environment page.

In order to use these credentials when fetching from the DPS API, you must set `CLIENT_ID` and `CLIENT_SECRET` environment variables in you shell before running the application with sbt.

We do not encourage you to add these credentials directly to the `application.conf` due to the risk of credentials being committed accidentally.

In order to call the real HIP API, comment out the initial set of configuration variables in the `microservice.services.dps-api`  block, and uncomment the second set of variables.

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
(remember to change it back after testing!)

### C. Confirm The Import Was Successful

You can review the logging of the service to ensure that each import job completed successfully.

If a job completed successfully it will emit a log statement like this:

```shell
2025-07-29 11:29:08,421 level=[INFO] logger=[uk.gov.hmrc.crdlcache.schedulers.ImportCorrespondenceListsJob] thread=[application-pekko.actor.default-dispatcher-15] rid=[] user=[] message=[import-correspondence-lists job completed successfully]
```

If you have not called the **crdl-cache** service locally before, you will need to set up a dummy internal-auth token by calling the test-only token endpoint of **internal-auth**:

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

Finally, you can use curl to fetch the data from the **crdl-cache**: 

```shell
curl -H 'Authorization crdl-cache-token' http://localhost:7252/crdl-cache/lists/CL012
```
