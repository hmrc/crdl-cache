
# crdl-cache

This service provides an API and caching layer for transit and excise reference data.

The data originates from the EU CS/RD2 and SEED reference data systems and is hosted at DPS (Data Platform Services).

This service exists to reduce the load on the DPS reference data APIs by caching reference data within MDTP.

## Prerequisites

To ensure that you have all the prerequisites for running this service, follow the Developer setup instructions in the MDTP Handbook.

This should ensure that you have the prerequisites for the service installed:

* JDK 21
* sbt 1.10.x or later
* MongoDB 7.x or later
* Service Manager 2.x

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").