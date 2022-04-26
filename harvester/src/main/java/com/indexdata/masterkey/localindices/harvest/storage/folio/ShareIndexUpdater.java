package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import com.indexdata.masterkey.localindices.util.MarcToJson;
import com.indexdata.masterkey.localindices.util.MarcXMLToJson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ShareIndexUpdater extends FolioRecordUpdater {

  ShareIndexUpdateContext ctxt;
  private final FailedRecordsController failedRecordsController;

  public ShareIndexUpdater(ShareIndexUpdateContext ctxt) {
    this.ctxt = ctxt;
    logger = ctxt.logger;
     failedRecordsController = ctxt.failedRecordsController;

    // updateCounters = ctxt.updateCounters;
  }

  @Override
  public void addRecord(RecordJSON recordJSON) {
    logger.debug("Adding record using ReShare Index updater");
    long startStorageEntireRecord = System.currentTimeMillis();

    try
    {
      if (recordJSON == null) throw new InventoryUpdateException ("The Shared Index storage logic " +
              "received no record from the transformation pipeline");
      TransformedRecord transformedRecord = new TransformedRecord(recordJSON, ctxt.logger);
      if ( transformedRecord.isRecordExcludedByDateFilter(ctxt) )
      {
        logger.debug("Record excluded by date filter");
      }
      else
      {
        JSONObject inventoryRecordSet = new JSONObject();
        JSONObject instance = transformedRecord.getInstance();
        if (instance == null) throw new InventoryUpdateException("The RSI storage logic " +
              "found no Instance object in the record from the transformation pipeline");
        if (instance.get("title") == null) throw new InventoryUpdateException("The RSI storage logic " +
                "found no 'title' in the Instance object in the record from the transformation pipeline");
        if ( transformedRecord.hasMatchKey())
        {
          instance.put( "matchKey", transformedRecord.getMatchKey() );
        }
        inventoryRecordSet.put( "instance", instance );
        if ( transformedRecord.getHoldings() != null )
        {
          inventoryRecordSet.put( "holdingsRecords", transformedRecord.getHoldings() );
        }
        JSONObject marcJson = getMarcJson(transformedRecord);
        if (marcJson == null) throw new InventoryUpdateException("The Shared Index storage logic " +
                "found no source MARC in the record from the transformation pipeline");
        JSONObject ingestRecordRequest = new JSONObject();
        JSONArray records = new JSONArray();
        JSONObject record = new JSONObject();
        if (ctxt.sourceId != null && !ctxt.sourceId.isEmpty()) {
          ingestRecordRequest.put("sourceId", ctxt.sourceId);
        } else if (transformedRecord.getInstitutionId() != null &&
                !transformedRecord.getInstitutionId().isEmpty()) {
          ingestRecordRequest.put("sourceId", transformedRecord.getInstitutionId());
        } else {
          throw new InventoryUpdateException("No source ID found in neither job configuration" +
                  " nor record. Source ID is mandatory in shared index, cannot ingest.");
        }
        record.put("localId", transformedRecord.getLocalIdentifier());
        record.put("marcPayload", marcJson);
        record.put("inventoryPayload", inventoryRecordSet);
        records.add(record);
        ingestRecordRequest.put("records", records);
        logger.debug("Created request JSON: " + ingestRecordRequest.toJSONString());
        updateSharedIndexEntry(ingestRecordRequest);
        ctxt.timingsIndexEntry.time(startStorageEntireRecord);
        ctxt.timingsCreatingRecord.setTiming( recordJSON.getCreationTiming() );
        ctxt.timingsTransformingRecord.setTiming( recordJSON.getTransformationTiming() );
        ctxt.storageStatus.incrementAdd( 1 );
      }
    } catch (InventoryUpdateException iue) {
      logger.error (iue.getMessage());
    }

  }

  private void updateSharedIndexEntry(JSONObject record) throws InventoryUpdateException {
    try {
      String url = ctxt.folioAddress + ctxt.sharedIndexPath;
      HttpEntityEnclosingRequestBase httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(record.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      ctxt.setHeaders(httpUpdate,"application/json");
      CloseableHttpResponse response = ctxt.folioClient.execute(httpUpdate);
      if (response.getStatusLine().getStatusCode() != 200) {
        logger.error(EntityUtils.toString(response.getEntity()));
      }
      response.close();
    } catch ( StorageException se) {
      throw se;
    } catch (Exception e) {
      throw new InventoryUpdateException("Shared Index updater encountered an exception: "
              + e.getMessage());
    }
  }


  @Override
  public void deleteRecord(RecordJSON recordJSON) {

  }

  /**
   * Create JSONObject from the XML in the incoming record
   * @param record
   * @return JSON formatted MARC record
   */
  private JSONObject getMarcJson(TransformedRecord record)  {
    JSONObject marcJson = null;
    if (record.getOriginalContent() != null) {
      try {
        String originalContentString = new String(record.getOriginalContent(), StandardCharsets.UTF_8 );
        if(originalContentString.startsWith("<") ) {
          logger.log(Level.TRACE,"Treating source record as XML");
          marcJson = MarcXMLToJson.convertMarcXMLToJson(originalContentString);
        } else {
          logger.log(Level.TRACE,"Treating source record as ISO2079");
          marcJson = MarcToJson.convertMarcRecordsToJson(record.getOriginalContent()).get(0);
        }
        logger.log(Level.TRACE,marcJson.toJSONString());
      } catch ( IOException | ParserConfigurationException | SAXException e) {
        RecordError error = new ExceptionRecordError(e, "Error creating MARC JSON for source record", "MARC source", "GET", "");
        // recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    }
    return marcJson;
  }

}
