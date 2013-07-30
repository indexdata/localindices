# Overview #

This document describes the Harvester using the following notation:

*   _Resource_: an external data source to harvest ADD TEXT

*   _Harvest Job_: the actual instance of harvesting a gievn resource with a specified schedule, as configured in the harvester

*   _Storage Engine_: or storage for short, a target index for the harvested data

*   _Transformation Step_: or step, which transforms data from one format to another or performs normalizations on the same format

*   _Transformation Pipeline_: or transformation,, which consist of an ordered list of multiple Transformation Steps

A Harvester Job fetches data via the HTTP protocol from a remote Resource, does some initial extraction/conversion of records depending on the protocol and configuration, splits the data into minor chunks (necessary on larger datasets due to memory constraints), sends this data through a Transformation Pipeline, and then stores the data into a Storage Engine for indexing.

The indexed resources are exposed to a Index Data MasterKey 2 meta-search platform and may be configured for searching in the  MasterKey 2 search UI.
