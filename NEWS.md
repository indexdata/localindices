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
