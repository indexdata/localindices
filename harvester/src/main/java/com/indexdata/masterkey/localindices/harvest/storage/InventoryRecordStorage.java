package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus.TransactionState;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.json.simple.JSONArray;

/**
 *
 * @author kurt
 */
public class InventoryRecordStorage implements RecordStorage {
  private String okapiUrl;
  private String folioUsername;
  private String folioPassword;
  private String folioTenant;
  private String authToken;
  private CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  private String database;
  private Map<String, String> databaseProperties;
  private StorageStatus storageStatus;
  private Date transactionStart;
  private List<String> transactionIdList;

  private String instanceTypeId = "6312d172-f0cf-40f6-b27d-9fa8feaf332f"; //TODO - Implement a lookup for this



  public InventoryRecordStorage() {
  }

  public InventoryRecordStorage(Harvestable harvestable) {
    this.harvestable = harvestable;
  }

  public void init() {
    try {
      Storage storage = null;
      if(harvestable != null) {
        storage = harvestable.getStorage();
      }
      logger = new FileStorageJobLogger(InventoryRecordStorage.class, storage);
    } catch(Exception e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void begin() throws IOException {
    logger.info("Transaction begin request recieved");
    database = harvestable.getId().toString();
    transactionStart = new Date();
    transactionIdList = new ArrayList<String>();
    storageStatus.setTransactionState(TransactionState.InTransaction);
  }

  @Override
  public void commit() throws IOException  {
    logger.info("Commit request recieved");
    storageStatus.setTransactionState(StorageStatus.TransactionState.Committed);
  }

  @Override
  public void rollback() throws IOException {
    //TODO: Issue deletes for all of the IDs in the transactionIdList
    logger.info("Rollback request recieved");
  }

  @Override
  public void purge(boolean commit) throws IOException {
    logger.info("Purge request recieved");
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    throw new UnsupportedOperationException("set overwrite mode Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean getOverwriteMode() {
    throw new UnsupportedOperationException("get overwrite mode Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    logger.info("Database started: " + database + ", with properties " + properties);
    this.databaseProperties = properties;
    this.database = database;
    try {
      client = HttpClients.createDefault();
      okapiUrl = "http://10.0.2.2:9130";
      folioUsername = InventoryRecordStorage.this.getConfigurationValue("folioUsername");
      folioPassword = InventoryRecordStorage.this.getConfigurationValue("folioPassword");
      folioTenant   = getConfigurationValue("folioTenant", "diku");
      if (folioUsername != null && folioPassword != null && folioTenant != null) {
        authToken = getAuthtoken(client, folioUsername, folioPassword, folioTenant);
      } else {
        logger.warn("Init Inventory storage: Missing one or more pieces of FOLIO authentication information. "
                + "Will attempt to continue without for tenant [" + folioTenant + "]."
                + "This would only work for a FOLIO instance with no authentication enabled.");
      }
      storageStatus = new InventoryStorageStatus(okapiUrl, authToken);
    } catch (StorageException se) {
      throw se;
    } catch (IOException ioe) {
      throw new StorageException("IO exception setting up access to FOLIO ", ioe);
    }

  }

  private String getConfigurationValue(String key) {
    String value = null;
    if (harvestable != null) {
      String configurationsJsonString = harvestable.getJson();
      if (configurationsJsonString != null && configurationsJsonString.length()>0) {
      try {
        JSONParser parser = new JSONParser();
        JSONObject configurations = (JSONObject) parser.parse(configurationsJsonString);
        value = (String) configurations.get(key);
      } catch (ParseException pe) {
        logger.warn("Could not parse JSON configuration from harvestable.json [" + configurationsJsonString + "]");
      }
      if (value == null) {
          logger.warn("Did not find value for key [" + key + "] in 'configurations JSON': " + configurationsJsonString);
        }
      } else {
        logger.warn("Cannot find value for key [" + key + "] because harvestable.json is empty");
      }
    } else {
      logger.warn("Cannot find value for key [" + key + "] from 'harvestable.json' because harvestable is null");
    }
    return value;
  }

  private String getConfigurationValue (String key, String defaultValue) {
    String value = InventoryRecordStorage.this.getConfigurationValue (key);
    return value != null ? value : defaultValue;
  }

  @Override
  public void databaseEnd() {
    logger.info("Database ended");
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    throw new UnsupportedOperationException("Adding record by key-values collection not supported.");
  }

  @Override
  public void add(Record recordJson) {
    if (recordJson.isCollection()) {
      Collection<Record> subrecords = recordJson.getSubRecords();
        for (Record subRecord : subrecords) {
          try {
            String addedId = addInstanceRecord(this.client, ((RecordJSON)subRecord).toJson(), this.folioTenant, this.authToken);
            ((InventoryStorageStatus) storageStatus).incrementAdd(1);
            transactionIdList.add(addedId);
          } catch(UnsupportedEncodingException uee) {
            ((InventoryStorageStatus) storageStatus).incrementAdd(0);
            logger.error("Encoding error when adding record: " + uee.getLocalizedMessage(), uee);
          } catch(IOException ioe) {
            ((InventoryStorageStatus) storageStatus).incrementAdd(0);
            logger.error("IO exception when adding record: " + ioe.getLocalizedMessage());
          } catch(ParseException pe) {
            ((InventoryStorageStatus) storageStatus).incrementAdd(0);
            logger.error("Parse exception when adding record: " + pe.getLocalizedMessage());
          }
        }
    } else {
      try {
        String addedId = addInstanceRecord(this.client, ((RecordJSON)recordJson).toJson(), this.folioTenant, this.authToken);
        ((InventoryStorageStatus) storageStatus).incrementAdd(1);
        transactionIdList.add(addedId);
      } catch(UnsupportedEncodingException uee) {
        ((InventoryStorageStatus) storageStatus).incrementAdd(0);
        logger.error("Encoding error when adding record: " + uee.getLocalizedMessage(), uee);
      } catch(IOException ioe) {
        ((InventoryStorageStatus) storageStatus).incrementAdd(0);
        logger.error("IO exception when adding record: " + ioe.getLocalizedMessage());
      } catch(ParseException pe) {
            ((InventoryStorageStatus) storageStatus).incrementAdd(0);
            logger.error("Parse exception when adding record: " + pe.getLocalizedMessage());
      }
    }
  }

  @Override
  public Record get(String id) {
    JSONObject instanceJson = null;
    try {
      instanceJson = getInstanceRecord(this.client, id, this.folioTenant,
        this.authToken);
    } catch(Exception e) {
      logger.error(String.format("Error getting record with id %s: %s ", id,
          e.getLocalizedMessage(), e));
      e.printStackTrace();
    }
    return recordFromInstanceJson(instanceJson);
  }

  @Override
  public void delete(String id) {
    try {
      deleteInstanceRecord(this.client, id, this.folioTenant, this.authToken);
    } catch (Exception e) {
      logger.error(String.format("Error deleting record with id %s: %s ", id,
          e.getLocalizedMessage(), e));
      e.printStackTrace();
    }
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    throw new UnsupportedOperationException("set logger Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.storageStatus;
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  @Override
  public void shutdown() throws IOException {
    throw new UnsupportedOperationException("shutdown Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("set batch limit Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  private Record recordFromInstanceJson(JSONObject instanceJson) {
    Map<String, Collection<Serializable>> values = new HashMap<>();
    RecordImpl recordImpl;


    recordImpl = new RecordImpl(values);
    return recordImpl;
  }

  private JSONObject makeInstanceJson(Record record) {

    JSONObject instanceJson = new JSONObject();
    Map<String, Collection<Serializable>> values = record.getValues();
    logger.info("record,values: " + values);

    Serializable title = values.containsKey("title") ? values.get("title").iterator().next() : "";
    instanceJson.put("title", title);

    instanceJson.put("instanceTypeId", "6312d172-f0cf-40f6-b27d-9fa8feaf332f");
    instanceJson.put("source", "HARVEST");

    if (values.containsKey("author")) {
      JSONArray contributors = new JSONArray();
      for (Serializable author : values.get("author")) {
        JSONObject contributor = new JSONObject();
        contributor.put("name", author);
        contributor.put("contributorNameTypeId", "2b94c631-fca9-4892-a730-03ee529ffe2a");
        contributor.put("primary", true);
        contributor.put("contributorTypeId", "6e09d47d-95e2-4d8a-831b-f777b8ef6d81");
        contributors.add(contributor);
      }
      instanceJson.put("contributors", contributors);
    }

    if (values.containsKey("subject")) {
      JSONArray subjects = new JSONArray();
      for (Serializable subject : values.get("subject")) {
        subjects.add(subject);
      }
      instanceJson.put("subjects", subjects);
    }

    if (values.containsKey("description")) {
      JSONArray notes = new JSONArray();
      for (Serializable description : values.get("description")) {
        notes.add(description);

      }
      instanceJson.put("notes", notes);
    }

    if (values.containsKey("publication-name")) {
      JSONArray publication = new JSONArray();
      for (Serializable publicationName : values.get("publication-name")) {
        JSONObject publisher = new JSONObject();
        publisher.put("publisher", publicationName);
        publication.add(publisher);
      }
      instanceJson.put("publication", publication);
    }
    logger.info("Created instanceJson from record: "+instanceJson.toJSONString());
    return instanceJson;
  }

  /*
    Add a new instance record to the folio inventory storage backend. Return the
    ID for the newly added instance.
  */
  private String addInstanceRecord(CloseableHttpClient client, JSONObject record,
      String tenant, String authToken)
      throws UnsupportedEncodingException, IOException, ParseException {
    String url = okapiUrl + "/instance-storage/instances";
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(record.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpPost);
    logger.info("Status code: " + response.getStatusLine().getStatusCode() +
        " for POST of record: " + record.get("title"));
    response.close();

    if(response.getStatusLine().getStatusCode() != 201) {
      logger.error(String.format("Got error %s, %s adding record: %s",
              response.getStatusLine().getStatusCode(),
              response.getStatusLine().getReasonPhrase(),
              record.get("title")));
      throw new IOException(String.format("Error adding record %s: %s (%s)",
              record.get("title"),
              response.getStatusLine().getReasonPhrase(),
              response.getStatusLine().getStatusCode()));
    }
    HttpEntity responseEntity = response.getEntity();
    if(responseEntity != null) {
      String content = EntityUtils.toString(responseEntity);
      response.close();
      JSONParser parser = new JSONParser();
      JSONObject instanceJson = (JSONObject)parser.parse(content);
      String instanceId = null;
      if(instanceJson.containsKey("id")) {
        instanceId = (String) instanceJson.get("id");
        return instanceId;
      } else {
        throw new IOException(String.format("No id found in response JSON %s", content));
      }
    } else {
      throw new IOException(String.format("No content found in response"));
    }
  }

  private JSONObject getInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken)
      throws IOException, ParseException {
    String url = String.format("%s/instance-storage/%s", okapiUrl, id);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpGet);
    if(response.getStatusLine().getStatusCode() != 200) {
      throw new IOException(String.format("Got error retrieving record with id '%s': %s",
          id, EntityUtils.toString(response.getEntity())));
    }
    JSONObject recordJson;
    JSONParser parser = new JSONParser();
    recordJson = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
    return recordJson;
  }

  private void deleteInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken) throws IOException {
    String url = String.format("%s/instance-storage/%s", okapiUrl, id);
    HttpDelete httpDelete = new HttpDelete(url);
    httpDelete.setHeader("Accept", "application/json");
    httpDelete.setHeader("Content-type", "application/json");
    httpDelete.setHeader("X-Okapi-Token", authToken);
    httpDelete.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpDelete);
    if(response.getStatusLine().getStatusCode() != 204) {
      throw new IOException(String.format("Got error deleting record record with id '%s': %s",
          id, EntityUtils.toString(response.getEntity())));
    }
  }

  private String getAuthtoken(CloseableHttpClient client, String username,
      String password, String tenant)
      throws UnsupportedEncodingException, IOException, StorageException {
    String url = okapiUrl + "/bl-users/login";
    HttpPost httpPost = new HttpPost(url);
    JSONObject loginJson = new JSONObject();
    loginJson.put("username", username);
    loginJson.put("password", password);
    StringEntity entity = new StringEntity(loginJson.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpPost);
    if(response.getStatusLine().getStatusCode() != 201) {
      throw new StorageException(String.format("Got bad response obtaining authtoken: %s, %s",
          response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity())));
    }
    return response.getFirstHeader("X-Okapi-Token").getValue();
  }

  private String getResourceTypeUUID(CloseableHttpClient client, String name) {
    return UUID.randomUUID().toString();
  }

}
