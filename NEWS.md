

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

* Introduces query parameters object in data access layers layers
* Adds column for record level access control to STORAGE, HARVESTABLE, TRANSFORMATION, STEP
* Supports query `acl=<tag>` for storages, harvestables, transformations, steps
