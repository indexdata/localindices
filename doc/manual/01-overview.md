\newpage

# Overview #

This document describes the Harvester using the following notation:

*   _Resource_: the external data source to be harvested

*   _Harvest Job_: the actual instance of harvesting a given resource with a specified schedule, as configured in the harvester

*   _Storage Engine_: or storage for short, a target index for the harvested data 
<!---
    D: THIS IS NOT CLEAR. Is it a designated Solr index into which the harvested data is placed for future indexing and/or retrieval?
    Jakub: yes, although in principle it could be something else, e.g simple
    filesystem.
    DS: I think the answer is YES, but not sure what you by designated. The data from each job will become a (sub)database (the Z39.50 term) on that indexing server (currently only Solr). 
    Yes this will be retrival through exposed url in a Toroid]
-->

*   _Transformation Step_: or step, which transforms data from one format to another or performs normalizations within the same format

*   _Transformation Pipeline_: or transformation, which consists of an ordered list of multiple Transformation Steps

A _Harvester Job_ retrieves data via a designated protocol from a remote _Resource_, performs an initial extraction and conversion of records depending on the protocol and job configuration, splits the data into minor chunks (necessary on larger datasets due to memory constraints), runs this data through a _Transformation Pipeline_, and, finally, writes the data to a _Storage Engine_ for indexing.

The indexed resources are exposed via an Index Data MasterKey meta-search platform and may be configured for searching in the  MasterKey search UI, or used through MasterKey Connect. 
