/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
      client = HttpClients.createDefault();
      okapiUrl = "http://10.0.2.2:9130";
      folioUsername = "diku_admin";
      folioPassword = "admin";
      folioTenant = "diku";
      storageStatus = new InventoryStorageStatus(okapiUrl, authToken);
    } catch(Exception e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void begin() throws IOException {
    logger.info("Transaction begin request recieved");
    database = harvestable.getId().toString();
  }

  @Override
  public void commit() throws IOException {
    logger.info("Commit request recieved");
  }

  @Override
  public void rollback() throws IOException {
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
    logger.info("Database started");
    this.databaseProperties = properties;
    this.database = database;
  }

  @Override
  public void databaseEnd() {
    logger.info("Database ended");
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    logger.info("Adding records via map");
    try {
      JSONObject json = makeInstanceJson(keyValues);
      if (json.containsKey("title")) {
        addInstanceRecord(this.client, json, this.folioTenant,
                this.authToken);
      } else {
        logger.info("Skipping JSON without a title");
      }
    } catch(Exception e) {
      logger.error("Error adding record: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void add(Record record) {
    logger.info("Adding record " + record.toString());
    try {
      JSONObject json = makeInstanceJson(record);
      if (json.containsKey("title")) {
        addInstanceRecord(this.client, json, this.folioTenant,
                this.authToken);
      } else {
        logger.info("Skipping JSON without a title");
      }
    } catch(Exception e) {
      logger.error("Error adding record: " + e.getLocalizedMessage(), e);
      e.printStackTrace();
    }
  }

  @Override
  public Record get(String id) {
    throw new UnsupportedOperationException("get by id Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void delete(String id) {
    throw new UnsupportedOperationException("delete by id Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

  private JSONObject makeInstanceJson(Record record) {
    JSONObject instanceJson = new JSONObject();
    Map<String, Collection<Serializable>> values = record.getValues();

    for(String key : values.keySet()) {
      for (Serializable value : values.get(key)) {
        if (key.equals("title")) {
          instanceJson.put(key, value);
          instanceJson.put("instanceTypeId", "6312d172-f0cf-40f6-b27d-9fa8feaf332f");
          instanceJson.put("source", "HARVEST");
        }
      }
    }
    logger.info("Created instanceJson from record: "+instanceJson.toJSONString());
    return instanceJson;
  }

  private JSONObject makeInstanceJson(Map<String, Collection<Serializable>> keyValues) {
    JSONObject instanceJson = new JSONObject();
    for(String key : keyValues.keySet()) {
      for( Serializable value : keyValues.get(key)) {
        if (key.equals("title")) {
          instanceJson.put(key, value);
          instanceJson.put("instanceTypeId", "6312d172-f0cf-40f6-b27d-9fa8feaf332f");
          instanceJson.put("source", "HARVEST");
        }
      }
    }
    logger.info("Created instanceJson from key-values: "+instanceJson.toJSONString());
    return instanceJson;
  }

  private void addInstanceRecord(CloseableHttpClient client, JSONObject record,
      String tenant, String authToken)
      throws UnsupportedEncodingException, IOException {
    logger.info("About to POST: " + record.toJSONString());
    String url = okapiUrl + "/instance-storage/instances";
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(record.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", tenant);

    CloseableHttpResponse response = client.execute(httpPost);
    logger.info("Status code: "+response.getStatusLine().getStatusCode());
    if(response.getStatusLine().getStatusCode() != 201) {
      throw new IOException(String.format("Got error adding record: %s", record.toJSONString()));
    }
  }

  private JSONObject getInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken)
      throws IOException, ParseException {
    String url = String.format("%s/instances/%s", okapiUrl, id);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpGet);
    if(response.getStatusLine().getStatusCode() != 200) {
      throw new IOException(String.format("Got error retrieving record record with id '%s': %s",
          id, EntityUtils.toString(response.getEntity())));
    }
    JSONObject recordJson;
    JSONParser parser = new JSONParser();
    recordJson = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
    return recordJson;
  }

  private void deleteInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken) throws IOException {
    String url = String.format("%s/instances/%s", okapiUrl, id);
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
      throws UnsupportedEncodingException, IOException {
    String url = okapiUrl + "/bl-users/login";
    HttpPost httpPost = new HttpPost(url);
    JSONObject loginJson = new JSONObject();
    StringEntity entity = new StringEntity(loginJson.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpPost);
    if(response.getStatusLine().getStatusCode() != 201) {
      throw new IOException(String.format("Got bad response obtaining authtoken: %s, %s",
          response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity())));
    }
    return response.getFirstHeader("X-Okapi-Token").getValue();
  }

  private String getResourceTypeUUID(CloseableHttpClient client, String name) {
    return UUID.randomUUID().toString();
  }

}
