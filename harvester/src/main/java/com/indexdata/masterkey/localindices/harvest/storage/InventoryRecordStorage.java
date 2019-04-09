/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.httpclient.HttpClient;
import org.apache.http.HttpResponse;
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

  public InventoryRecordStorage() {

  }

  public InventoryRecordStorage(Harvestable harvestable) {
    this.harvestable = harvestable;
  }


  public void init() {
    try {
      Storage storage = harvestable.getStorage();
      logger = new FileStorageJobLogger(InventoryRecordStorage.class, storage);
      client = HttpClients.createDefault();
      okapiUrl = "http://localhost:9130";
      folioUsername = "diku_admin";
      folioPassword = "admin";
      folioTenant = "diku";
    } catch(Exception e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }
  

  @Override
  public void begin() throws IOException {
    logger.info("Transaction begin request recieved");
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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean getOverwriteMode() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void databaseEnd() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void add(Record record) {
    try {
      addInstanceRecord(this.client, makeInstanceJson(record), this.authToken,
          this.folioTenant);
    } catch(Exception e) {
      logger.error("Error adding record: " + e.getLocalizedMessage(), e);
      e.printStackTrace();
    }
  }

  @Override
  public Record get(String id) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void delete(String id) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void shutdown() throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  private JSONObject makeInstanceJson(Record record) {
    JSONObject instanceJson = new JSONObject();
    if(record.getId() != null) {
      instanceJson.put("recordId", record.getId());
    }
    Map<String, Collection<Serializable>> values = record.getValues();
    for(String key : values.keySet()) {
      for (Serializable value : values.get(key)) {
        instanceJson.put(key, value);
      }
    }
    return instanceJson;
  }

  private void addInstanceRecord(CloseableHttpClient client, JSONObject record,
      String tenant, String authToken)
      throws UnsupportedEncodingException, IOException {
    String url = okapiUrl + "/instances";
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(record.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpPost);
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
