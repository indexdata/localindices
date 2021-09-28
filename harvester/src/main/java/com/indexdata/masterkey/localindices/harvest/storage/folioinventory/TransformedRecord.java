package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;

/**
   * Represents the JSON coming out of the transformation pipeline, containing one or more Inventory
   * entities like Instance, holdings records, items, the original record, and other data needed for
   * the Inventory ingestion logic. <br/>
   * <br/>
   * Some slight variations in the structure are allowed so this facade is meant to shield the user of
   * class from that.
   * <br/>
   * The different structures supported: <br/>
   * <br/>
   * <ol>
   * <li>the "record" object contains an "instance" object and a separate "holdingsRecords"  array with embedded "items" arrays</li>
   * <li>the "record" object contains an "instance" object with an <i>embedded</i>  "holdingsRecords" array with embedded "items" arrays</li>
   * <li>the "record" object <i>is</i> the instance, containing an embedded "holdingsRecords" array with embedded "items" arrays</li>
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

    private RecordJSON recordJSON;
    private JSONObject transformed;
    private JSONParser parser = new JSONParser();
    private StorageJobLogger logger;

    public TransformedRecord(RecordJSON recordJSON, StorageJobLogger logger) {
      this.recordJSON = recordJSON;
      this.logger = logger;
      logger.log(Level.TRACE, "Got transformed record for storage/deletion: " + recordJSON.toJson().toJSONString());
      normalizeTransformedRecord();
      logger.log(Level.TRACE, "Cached JSON as " + transformed.toJSONString());
    }

    /**
     * Pulls record out of 'collection' and/or 'record' container(s)
     */
    private void normalizeTransformedRecord() {
      if (isCollectionOfOne(recordJSON.toJson())) {
        this.transformed = getOnlyCollectionItem(recordJSON.toJson());
      } else {
        this.transformed=recordJSON.toJson();
      }
      if (transformed.containsKey("record")) {
        transformed = (JSONObject) transformed.get("record");
      }
    }

    /**
     * Retrieves institution ID from element created during transformation if present
     * otherwise attempts to defer it from locations on holdings records.
     *
     * @param locationsToInstitutionsMap
     * @return Inventory institution UUID
     */
    public String getInstitutionId (Map<String,String> locationsToInstitutionsMap) {
      if (transformed.containsKey("institutionId")) {
        return getInstitutionId();
      } else {
        return getInstitutionId(getHoldings(), locationsToInstitutionsMap);
      }
    }

  /**
   * Retrieve institutionId from transformed record, null if none found
   * @return the institution ID or null
   */
  public String getInstitutionId () {
      return (String) transformed.get("institutionId");
    }

    public String getLocalIdentifier () {
      String id = (String) transformed.get("localIdentifier");
      if (id == null) id = (String) getInstance().get("hrid");
      return id;
    }

  /**
   * Get FOLIO's UUID for the type of the localIdentifier
   * @return the given library's identifierTypeId for its local identifiers
   */
  public String getIdentifierTypeId () {
      return (String) transformed.get("identifierTypeId");
    }

    public String getOriginalXml() {
      String original = null;
      if (transformed.get("original") == null) {
        logger.debug("Did not find property 'original' with transformed record");
      } else if (transformed.get("original") instanceof String
                 && isValidXml((String) transformed.get("original"))) {
        original = (String) transformed.get("original");
      } else if (transformed.get("original") instanceof JSONObject) {
        logger.warn("Found property named 'original' but it was a JSONObject (not a string of XML");
      }
      return original;
    }

    private boolean isValidXml (String maybeXml)  {
      try {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(maybeXml)));
        return true;
      } catch (IOException | ParserConfigurationException | SAXException e) {
        return false;
      }
    }

    public JSONObject getTransformedRecordExclusiveOriginal() {
      JSONObject transformedExclussiveOriginal = null;
      if (transformed != null) {
        try {
          JSONParser parser = new JSONParser();
          transformedExclussiveOriginal = (JSONObject) parser.parse(transformed.toJSONString());
          transformedExclussiveOriginal.remove("original");
        } catch (ParseException e) {
          logger.error("Error creating transformed record excluding original record: " + e.getMessage());
        }
      }
      return transformedExclussiveOriginal;
    }

    public byte[] getOriginalContent () {
      return this.recordJSON.getOriginalContent();
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

    /**
     *
     * @return a detached copy (not a reference) of the Instance JSON object in Record.
     */
    public JSONObject getInstance () {
      logger.log(Level.TRACE, "Looking for instance in root of " + transformed.toJSONString());
      JSONObject instance = new JSONObject();
      try {
        if (transformed.containsKey("instance")) {
          JSONObject instanceFromRecord = (JSONObject) (transformed.get("instance"));
          instance = (JSONObject) parser.parse(instanceFromRecord.toJSONString());
        } else {  // Assume record _is_ the instance
          instance = (JSONObject) parser.parse(transformed.toJSONString());
        }
        instance.remove("holdingsRecords");
        instance.remove("matchKey");
      } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to get Instance: " + pe.getMessage());
      }
      return instance;
    }

    /**
     *
     * @return a detached object (not a reference) of the JSON Array of holdings records from the Record.
     */
    public JSONArray getHoldings () {
      JSONArray holdings = null;
      try {
        if (transformed.containsKey("holdingsRecords")) {
          JSONArray holdingsRecordsFromRecord = (JSONArray) (transformed.get("holdingsRecords"));
          holdings = (JSONArray) parser.parse(holdingsRecordsFromRecord.toJSONString());
        } else if (transformed.containsKey("instance")) {
          JSONObject instance = (JSONObject) transformed.get("instance");
          if (instance.containsKey("holdingsRecords")) {
            JSONArray holdingsRecordsFromInstance = (JSONArray) (instance.get("holdingsRecords"));
            holdings = (JSONArray) parser.parse(holdingsRecordsFromInstance.toJSONString());
          }
        }
      } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to retrieve holdings: " + pe.getMessage());
      }
      return holdings;
    }

    public boolean hasInstanceRelations() {
      return transformed.containsKey("instanceRelations");
    }

    public JSONObject getInstanceRelations() {
      logger.log(Level.TRACE, "Looking for instance relations in root of " + transformed.toJSONString());
      JSONObject instanceRelations = new JSONObject();
      try {
        if (transformed.containsKey("instanceRelations")) {
          JSONObject instanceRelationsFromRecord = (JSONObject) (transformed.get("instanceRelations"));
          instanceRelations = (JSONObject) parser.parse(instanceRelationsFromRecord.toJSONString());
        }
      } catch (ParseException pe) {
        logger.error("InventoryRecordStorage could not parse transformed record to get Instance relations: " + pe.getMessage());
      }
      return instanceRelations;
    }

    public boolean hasMatchKey () {
      return !(getMatchKey().isEmpty());
    }

    public JSONObject getMatchKey () {
      JSONObject matchKey = new JSONObject();
      try {
        if (transformed.containsKey("matchKey")) {
          JSONObject matchKeyFromRecord = (JSONObject) (transformed.get("matchKey"));
          matchKey = (JSONObject) parser.parse(matchKeyFromRecord.toJSONString());
        } else if (transformed.containsKey("instance")) {
          JSONObject instance = (JSONObject) transformed.get("instance");
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
      return transformed.containsKey("delete");
    }

    public JSONObject getDelete () {
      if (isDeleted()) {
        return (JSONObject)(transformed.get("delete"));
      } else {
        return null;
      }
    }

    public JSONObject getJson () {
      return transformed;
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
      return transformed != null ? transformed.toJSONString() : "no record";
    }
  }
