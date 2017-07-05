\newpage

# Appendix 1 -- Harvester architecture  #

## Harvester Admin ##

The Harvester Admin is a JavaServer Faces web application that can be deployed
in a standard servlet container or Java application server (Tomcat is used by
default). The Admin web application communicates with the backend, Harvester Engine, through a RESTful XML-over-HTTP API. Harvester Admin has no persistent storage on its own. The Harvester API is attached to this manual as an appendix.

## Harvester Engine ##

The Harvester Engine is a Java JAX-RS web application that runs on a standard Java servlet container like Tomcat. The Engine exposes a RESTful API that allows
to create, configure, run and track harvest jobs. The Engine is the active
component in the harvesting process as it executes and monitors running jobs,
performs user-defined normalizations and transformations and redirects the output to a configured storage engine. Two kinds of storage engines are supported: Index Data's own Zebra and Solr/SolrCloud. SolrCloud is the preferred storage
solution that offers high reliability and data replication.

Index Data's own key-value meta-data format, MasterKey format, is used to store
transformed meta-data records in the Solr instance. The format includes a list
of fields modelled after the DublinCore format but can be extended and altered to include additional fields.

All persistent configuration, e.g for Harvest Jobs and Transformation Pipelines configuration, is stored in a mySQL database instance attached to the harvester Engine.


# Appendix 1 -- Harvester Core API  #

Below is the RAML spec file for Harvester Core "harvestables" API.

```
#%RAML 0.8
title: Harvester
version: 1.0
baseUri: http://harvester.example.com/harvester/records
documentation:
 - title: Overview
   content: >
     This API provides a means for clients to control a pool of
     "harvestables" -- harvesting tasks which are scheduled to run at
     specified times in order to maintain a live database of
     accumulated results.

     * The tasks in the pool may be listed using GET, and new tasks
       may be added using POST.

     * Individual tasks are addressable by means of an opaque
       identifier. They may be fetched with GET, updated with PUT or
       removed with DELETE.

/harvestables:
  get:
    description: >
      Return brief information about all harvesting jobs.
    responses:
      200:
        description: >
          A `<harvestables>` document containing zero or more
          `<harvestableBrief>` records, each of which carries
          information about a harvesting job. This information
          includes the `<currentStatus>` field.
        body:
          text/xml:
            example: |
              <harvestables count="68" max="100" uri="http://localhost:8080/harvester/records/harvestables/" start="0">
                <harvestableBrief uri="http://localhost:8080/harvester/records/harvestables/6503/">
                  <amountHarvested>0</amountHarvested>
                  <currentStatus>WARN</currentStatus>
                  <enabled>false</enabled>
                  <id>6503</id>
                  <jobClass>OaiPmhResource</jobClass>
                  <lastHarvestFinished>2015-05-25T12:07:56Z</lastHarvestFinished>
                  <lastHarvestStarted>2015-05-25T12:07:56Z</lastHarvestStarted>
                  <lastUpdated>2015-05-25T12:07:56Z</lastUpdated>
                  <message>No Records matched</message>
                  <name>A OAI-PMH test</name>
                  <nextHarvestSchedule>2016-03-19T00:00:00Z</nextHarvestSchedule>
                </harvestableBrief>
                ...
              </harvestables>

  post:
    description: >
      Create a new harvesting job. The format of the XML to be posted
      is the same as that returned by GET /harvestables/_id_, although
      most fields may be omitted.
  /{id}:
    uriParameters:
      id:
        description: >
          Opaque identifier of a specific harvesting job.
    get:
      description: >
        Retrieve details about, and status of, a specific harvesting job.
        Fields include:

        * **currentStatus**: set to `NEW` when a new job is created by
          POST; changes to `RUNNING` when a scheduled job begins, and
          to `FINISHED` when it completes successfully. If something
          goes wrong, the status is set to `ERROR` or `WARN`.

      responses:
        200:
          body:
            text/xml:
              example: |
                <harvestable uri="http://harvester.indexdata.com/harvester/records/harvestables/4902/">
                  <oaiPmh>
                  <id>4902</id>
                  <!-- user controlled meta-data follows, all but name is optional-->
                  <name>Sample OAI job</name>
                  <serviceProvider/>
                  <technicalNotes/>
                  <contactNotes/>
                  <description/>
                  <!-- connection options -->
                  <url>http://localhost:8080/harvester/oaipmh</url>
                  <!-- base URL for the target server -->
                  <retryCount>2</retryCount>
                  <retryWait>60</retryWait>
                  <timeout>60</timeout>
                  <allowErrors>true</allowErrors>
                  <!-- allow non-well formed XML parsing -->
                  <laxParsing>false</laxParsing>
                  <!-- schedule for periodic harvests, CRON-style -->
                  <scheduleString>0 11 * * 2</scheduleString>
                  <enabled>true</enabled>
                  <!-- enables interactive harvesting mode -->
                  <harvestImmediately>true/false</harvestImmediately>
                  <!-- storage options follows :-->
                  <!-- keep or remove data from previous runs -->
                  <overwrite>false</overwrite>
                  <!-- keep data for runs with errors -->
                  <keepPartial>true</keepPartial>
                  <logLevel>INFO</logLevel>
                  <!-- e-mail notification settings -->
                  <mailAddress/>
                  <mailLevel>WARN</mailLevel>
                  <!-- OAI options -->
                  <!-- from -->
                  <fromDate>2015-04-07T11:00:00.152Z</fromDate>
                  <!-- to -->
                  <untilDate>2015-04-07T11:00:00.152Z</untilDate>
                  <dateFormat>yyyy-MM-dd</dateFormat>
                  <!-- OAI date long or short format used -->
                  <metadataPrefix>oai_dc</metadataPrefix>
                  <oaiSetName>music</oaiSetName>
                  <!-- read-only status information follows -->
                  <!-- status ENUM for the polling client -->
                  <currentStatus>NEW</currentStatus>
                  <!-- exception message or empty for successful harvests -->
                  <message>No records matched</message>
                  <amountHarvested>0</amountHarvested>
                  <initiallyHarvested>2013-01-04T07:00:59Z</initiallyHarvested>
                  <lastHarvestFinished>2015-04-07T11:00:00.746Z</lastHarvestFinished>
                  <lastHarvestStarted>2015-04-07T11:00:00.152Z</lastHarvestStarted>
                  <lastUpdated>2015-02-03T14:15:54Z</lastUpdated>
                  </sushiJob>
                </harvestable>
    delete:
      description: >
        Remove a harvesting job, cancelling its current run if it's active.
    put:
      description: >
        Update details of the harvesting job. Doing this while the job
        is running will force the present run to be abandoned, and the
        harvest to be restarted.
```
