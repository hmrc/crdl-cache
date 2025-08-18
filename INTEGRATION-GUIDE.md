# Integrating with the Central Reference Data Library

## Why?

You should use the Central Reference Data Library if your service needs to integrate with:
* Customs and transit reference data from the EU CS/RD2 system
* Excise reference data from the EU SEED system

crdl-cache provides an API and caching layer for reference data hosted at DPS (Data Platform Services) for services hosted on MDTP.

## What?

### Codelists

Reference data takes the form of "codelists", which are usually lists of mappings between codes and values.

Codelists have various uses within the customs and excise systems, from populating drop downs to describing the relationships between the entries of other codelists.

For example, EMCS services make use of the packaging types (BC17) codelist when describing how items in an excise movement are packaged.

Some codelist entries contain additional information beyond keys and values, so we represent our data as lists of codelist entries with additional `properties`, for example:

```json
[
  {
    "key": "1A",
    "value": "Drum, steel",
    "properties": {
      "countableFlag": true,
      "actionIdentification": "1236"
    }
  },
  {
    "key": "1B",
    "value": "Drum, aluminium",
    "properties": {
      "countableFlag": true,
      "actionIdentification": "1237"
    }
  }
]
```

At present we offer filtering on the keys and additional properties of codelist entries.

Please see the [API Documentation](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml) for crdl-cache for more details about fetching codelist entries and their representation.

### Customs offices

There is one notably more complex codelist - the customs office list (CL141). We treat the customs office list as a special case and provide separate endpoints for this data in crdl-cache.

Customs offices are used in many different systems. At present we offer filtering on the reference numbers, country codes and roles of an office.

Please see the [API Documentation](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml) for crdl-cache for more details about fetching customs offices and their representation.

## How?

Please contact the owners of crdl-cache in the [MDTP Catalogue](https://catalogue.tax.service.gov.uk/service/crdl-cache) to discuss integrating with the service.

You will need some estimate of the additional load that your service will place upon crdl-cache.

This is so that we can update our performance tests and anticipate whether the service may need to be resized to handle this additional load.

You will also need to set up the [internal-auth](https://github.com/hmrc/internal-auth) system to allow your service to call crdl-cache.

To do this:
* Add your service to the grantees for crdl-cache in the [internal-auth-config](https://github.com/hmrc/internal-auth-config/blob/5589546c4cba9ef79c426d9fa53780a0e5249c06/qa/grants.yaml#L776).
* Raise a ticket with #team-platops to create an internal-auth token for your service.
* Once this internal-auth token is added to your service configuration, use it in the `Authorization` header when calling crdl-cache.

## Acceptance testing

For the purpose of acceptance testing, it may be difficult to deal with the lifecycle of data in the crdl-cache service.

This service fetches its data using a regular import process, so it will not be filled with data immediately as soon as it is started using service-manager.

To support acceptance testing, we provide a local-only stub [crdl-cache-stub](https://github.com/hmrc/crdl-cache-stub) which serves a full set of reference data from CRDL.

All of the filtering parameters of crdl-cache are supported and the service behaves as similarly as possible to crdl-cache with some [minor differences](https://github.com/hmrc/crdl-cache-stub?tab=readme-ov-file#differences).
