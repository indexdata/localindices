## 2.15.7 2024-06-01
 * FOLIO storage: support for login-with expiry (MODHAADM-91) 

## 2.15.6 2024-05-28
 * Annul obsolete log statement (MODHAADM-86)
 * Include counts of skipped deletes in status message (MODHAADM-89)

## 2.15.5 2024-01-22
 * Maintenance (obsolete dependency with vulnerability, outdated dev tools) 

## 2.15.4 2024-01-20
 * Bug fix, FOLIO batch update, preserve order of updates, deletes in incoming files (MKH-548, MODHAADM-83)

## 2.15.3 2023-11-14
 * Bug fix, add proper DTD to Jetty env XML config
 * Add Maven 'clean' step prior to 'package' in Dockerfile

## 2.15.2 2023-11-05
 * Bug fix, inventory delete URL when using batch upsert (MKH-546, MODHAADM-75)
 * Increase column size for transformation scripts (`STEP.SCRIPT`) (MKH-547)

## 2.15.1 2023-08-11
 * Update classname of MySQL 8 connector in Docker Jetty config
 * Add new top-level Dockerfile which will build both harvester and harvester-admin Docker images
 * Add Docker README (README-Docker.md)

## 2.15.0 2023-07-06

 * FOLIO storage: XML-to-JSON, transforms empty element to property with value null (MKH-543)
 * Upgrades MySQL client to 8.0.32, Log4j to version 2 (MODHAADM-27)
 * FOLIO storage: Supports Inventory batch upsert
 * FOLIO storage: adds option to save logs in FOLIO (MODHAADM-30) 
 * Adds support for harvesting 7Zip files (MKH-535)
 * MARC-to-JSON: Handles (any or no) namespace on the 'record' field (DEVOPS-1198)
 * FOLIO storage: Adds option to filter XML bulk records by date (MKH-537)
 * Support queries for TSAS entities (step associations)
 * Properly encode query parameters. 
 * Always convert MARC binary to UTF-8.
 * Workarounds to deal with FTP server timeouts for large files. PR-818.

## 2.14.0

 * Adds support for raw MARC file import to FOLIO Inventory
 * Adds simple webservice serving up failed records and error information from file system
 * Stores failed records in logging directories, according to choices made in job configuration
 * Reinstates option to ingest without FOLIO auth enabled
 * Adds support for new Inventory storage API: upsert APIs served by mod-inventory-update
 * Adds conditional conversion of JSON for iso2079 vs XML

## 2.13.0

 * Introduces text column `JSON` on STORAGE for free form STORAGE configuration (GBV-56)
 * Moves Inventory storage configuration from `HARVESTABLE` to `STORAGE` (GBV-56)
 * Adds documentation for the conventions for transformation to inventory records
 * Supports updates and deletes of MARC source records PR-582
 * Adds option for TRACE level logging
 * Adds delete signal detection
 * Adds query capability in web service / data access layer of the configuration database
 * Improves error-handling, -counting, and -reporting

## 2.12.0

* Introduces query parameters object in data access layers
* Adds column for record level access control to STORAGE, HARVESTABLE, TRANSFORMATION, STEP
* Supports query `acl=<tag>` for storages, harvestables, transformations, steps
