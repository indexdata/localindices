/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.Map;

import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 *
 * @author ne
 */
/**
   * Represents the JSON coming out of the transformation pipeline, containing an instance, <br/>
   * and possibly holdings and item. <br/>
   * <br/>
   * A couple of different structures are supported: <br/>
   * <br/>
   * <ol>
   * <li>the "record" object contains an "instance" object and a "holdingsRecords"  array with embedded "items" arrays</li>
   * <li>the "record" object contains an "instance" object with an <i>embedded</i> "holdingsRecords" array with embedded "items" arrays</li>
   * <li>the "record" object _is_ the instance, containing an embedded "holdingsRecords" array with embedded "items" arrays</li>
   * </ol>
   * <pre>
   *  "record" contains
   *    "instance" object and
   *    "holdingsRecords" array of holdings records
   *       with embedded
   *       "items" arrays
   *
   * or
   *
   *  "record" contains
   *     "instance" object
   *        with embedded
   *        "holdingsRecords" array of holdings records
   *             with embedded
   *             "items" arrays
   * or
   *
   *  "record" contains
   *      instance properties
   *      and an embedded
   *      "holdingsRecords" array of holdings records
   *          with embedded
   *          "items" arrays
   * </pre>
   *
   *
   * In the first two cases, the "record" may contain other elements than "instance" and "holdingsRecords", for example if the
   * transformation pipeline created additional data for use in the pipeline and did not clean them up again. Such extra elements
   * will be ignored. In the third case, however, any elements in the "record" that are not valid instance properties would make
   * ingestion fail -- except for the expected "holdingsRecords" array.
   *
   */
  public class TransformedRecord {

    private JSONObject record;
    private JSONParser parser = new JSONParser();
    private StorageJobLogger logger;

    public TransformedRecord(JSONObject recordJson, StorageJobLogger logger) {
      this.logger = logger;
      logger.log(Level.TRACE, "Got transformed record for storage/deletion: " + recordJson.toJSONString());
      if (isCollectionOfOne(recordJson)) {
        this.record = getOnlyCollectionItem(recordJson);
      } else {
        this.record=recordJson;
      }
      logger.log(Level.TRACE, "Cached JSON as " + record.toJSONString());
    }

    public String getInstitutionId (Map locationsToInstitutionsMap) {
      if (record.containsKey("institutionId")) {
        return (String) record.get("institutionId");
      } else {
        return getInstitutionId(getHoldings(), locationsToInstitutionsMap);
      }
    }

    public String getLocalIdentifier () {
      return (String) record.get("localIdentifier");
    }

    private String getInstitutionId(JSONArray holdingsRecords, Map<String,String> locationsToInstitutionsMap) {
      String locationId = getLocationId(holdingsRecords);
      if (locationId != null) {
        return locationsToInstitutionsMap.get(locationId);
      } else {
        return null;
      }
    }

    public String getLocationId(JSONArray holdingsRecords) {
      if (holdingsRecords != null && !holdingsRecords.isEmpty() && holdingsRecords.get(0) instanceof JSONObject ) {
        return (String)((JSONObject)(holdingsRecords.get(0))).get("permanentLocationId");
      } else {
        return null;
      }
    }

    public JSONObject getInstance () {
      logger.log(Level.TRACE, "Looking for instance in root of " + record.toJSONString());
      JSONObject instance = new JSONObject();
      try {
        if (record.containsKey("instance")) {
          JSONObject instanceFromRecord = (JSONObject) (record.get("instance"));
          instance = (JSONObject) parser.parse(instanceFromRecord.toJSONString());
        } else {  // Assume record _is_ the instance
          instance = (JSONObject) parser.parse(record.toJSONString());
        }
        instance.remove("holdingsRecords");
        instance.remove("matchKey");
    } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to get Instance: " + pe.getMessage());
      }
      return instance;
    }

    public JSONArray getHoldings () {
      JSONArray holdings = new JSONArray();
      try {
        if (record.containsKey("holdingsRecords")) {
          JSONArray holdingsRecordsFromRecord = (JSONArray) (record.get("holdingsRecords"));
          holdings = (JSONArray) parser.parse(holdingsRecordsFromRecord.toJSONString());
        } else if (record.containsKey("instance")) {
          JSONObject instance = (JSONObject) record.get("instance");
          if (instance.containsKey("holdingsRecords")) {
            JSONArray holdingsRecordsFromInstance = (JSONArray) (instance.get("holdingsRecords"));
            holdings = (JSONArray) parser.parse(holdingsRecordsFromInstance.toJSONString());
          } else {
            logger.warn("InventoryRecordStorage could not find `holdingsRecord` anywhere in transformed record");
          }
        }
      } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to retrieve holdings: " + pe.getMessage());
      }
      return holdings;
    }

    public JSONObject getMatchKey () {
      JSONObject matchKey = new JSONObject();
      try {
        if (record.containsKey("matchKey")) {
          JSONObject matchKeyFromRecord = (JSONObject) (record.get("matchKey"));
          matchKey = (JSONObject) parser.parse(matchKeyFromRecord.toJSONString());
        } else if (record.containsKey("instance")) {
          JSONObject instance = (JSONObject) record.get("instance");
          if (instance.containsKey("matchKey")) {
            JSONObject matchKeyFromInstance = (JSONObject) (instance.get("matchKey"));
            matchKey = (JSONObject) parser.parse(matchKeyFromInstance.toJSONString());
          }
        }
      } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to retrieve matchKey: " + pe.getMessage());
      }
      return matchKey;
    }

    public boolean isDeleted () {
      return record.containsKey("delete");
    }

    public JSONObject getDelete () {
      if (isDeleted()) {
        return (JSONObject)(record.get("delete"));
      } else {
        return null;
      }
    }

    private boolean isCollectionOfOne (JSONObject json) {
      return (json.containsKey("collection")
        && json.get("collection") instanceof JSONArray
        && ((JSONArray) (json.get("collection"))).size() == 1);
    }

    private JSONObject getOnlyCollectionItem (JSONObject json) {
      return (JSONObject) (((JSONArray) json.get("collection")).get(0));
    }

    @Override
    public String toString() {
      return record != null ? record.toJSONString() : "no record";
    }
  }
