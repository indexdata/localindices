# Overview #

This document describes the Harvester using the following notation:

*   _Resource_: the external data source to be harvested

*   _Harvest Job_: the actual instance of harvesting a given resource with a specified schedule, as configured in the harvester

*   _Storage Engine_: or storage for short, a target index for the harvested data [THIS IS NOT CLEAR. Is it a designated Solr index into which the harvested data is placed for future indexing and/or retrieval?]

*   _Transformation Step_: or step, which transforms data from one format to another or performs normalizations within the same format

*   _Transformation Pipeline_: or transformation, which consists of an ordered list of multiple Transformation Steps

A Harvester Job fetches data via the HTTP protocol from a remote Resource, does some initial extraction/conversion of records depending on the protocol and configuration, splits the data into minor chunks (necessary on larger datasets due to memory constraints), sends this data through a Transformation Pipeline, and then writes the data to a Storage Engine for indexing.

The indexed resources are exposed via an Index Data MasterKey meta-search platform and may be configured for searching in the  MasterKey search UI, or used through MasterKey Connect. 
