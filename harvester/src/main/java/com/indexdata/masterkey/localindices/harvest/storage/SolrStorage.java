package com.indexdata.masterkey.localindices.harvest.storage;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 * A simple utility class for posting raw updates to a Solr server, has a main
 * method so it can be run on the command line.
 * 
 */
public class SolrStorage implements HarvestStorage {
  public String POST_ENCODING = "UTF-8";
  public String VERSION_OF_THIS_TOOL = "1.2";
  protected String url = "http://localhost:8983/solr/";
  protected SolrServer server;
  protected Harvestable harvestable;
  protected StorageJobLogger logger;
  ByteArrayOutputStream output = new ByteArrayOutputStream();
  protected Collection<SolrInputDocument> documentList = null;
  private boolean override = false;
  String storageId = "";  
  protected SolrStorageStatus storageStatus;
  String databaseField = "database:";
  
  public SolrStorage() {
  }

  public SolrStorage(Harvestable harvestable) {
    setHarvestable(harvestable);
  }

  public SolrStorage(String solrUrl, Harvestable harvestable) {
    this.harvestable = harvestable;
    url = solrUrl;

    init();
  }

  public SolrStorage(SolrServer server, Harvestable harvestable) {
    this.harvestable = harvestable;
    this.server = server; 
    Storage storage = null;
    if (harvestable != null) {
      storage = harvestable.getStorage();
    }
    logger = new FileStorageJobLogger(SolrStorage.class, storage);
    storageStatus = new SolrStorageStatus(server, databaseField + harvestable.getId());
  }

  public void init() {
    try {
      Storage storage = null;
      if (harvestable != null) {
        storage = harvestable.getStorage();
      }
      if (storage == null) {
	throw new RuntimeException("Fail to init Storage " + this.getClass().getCanonicalName() 
	    	+ " No Storage Entity on Harvestable(" + harvestable.getId() + " - " + harvestable.getName() + ")");
      }
      setStorageId(storage.getId().toString());
      url = storage.getUrl();
      logger = new FileStorageJobLogger(SolrStorage.class, storage);
      //server = new StreamingUpdateSolrServer(url, 1000, 10);
      ConcurrentUpdateSolrServer server = new ConcurrentUpdateSolrServer(url, 100, 10);
      server.setSoTimeout(100000); // socket read timeout
      server.setConnectionTimeout(10000);
      server.setParser(new XMLResponseParser());
      this.server = server;
      
      storageStatus = new SolrStorageStatus(server, databaseField + harvestable.getId());
    } catch (Exception e) {
      throw new RuntimeException("Unable to init Solr Server: " + e.getMessage(), e);
    }
  }

  @Override
  public void begin() throws IOException {

    documentList = new LinkedList<SolrInputDocument>();
    output.reset();

  }

  @Override
  public void commit() throws IOException {
    SolrXmlParser parser = new SolrXmlParser();
    SolrXmlHandler context = new SolrXmlHandler();
    try {
      parser.parse(output.toString(), context);
    } catch (XMLStreamException e) {
      logger.error("SolrXmlParser: " + e.getMessage());
      e.printStackTrace();
      throw new IOException("Error in Solr Xml parse: " + e.getMessage(), e);
    }
    try {
	logger.debug("Document: " + context.getDocuments());
	UpdateResponse response = server.add(context.getDocuments());
	logger.info("UpdateResponse: " + response.getStatus() + " " + response.getResponse());
	response = server.commit();
        logger.info("CommitResponse: " + response.getStatus() + " " + response.getResponse());
	storageStatus.setTransactionState(StorageStatus.TransactionState.Committed);

    } catch (SolrServerException e) {
      logger.error("Solr Server Exception: " + e.getMessage());
      logger.debug(e.getStackTrace().toString());
      throw new IOException("Error in SOLR add/commit", e);
    }
  }

  @Override
  public void rollback() throws IOException {
    try {
      // TODO rewrite rollback to use purgeByTransactionId()
      server.rollback();
    } catch (SolrServerException e) {
      e.printStackTrace();
      throw new RuntimeException("Solr Server Exception in SOLR Rollback: " + e.getMessage(), e);
    }
  }

  @Override
  public void purge(boolean commit) throws IOException {
    try {
      UpdateResponse response = server.deleteByQuery(databaseField + harvestable.getId());
      logger.info("UpdateResponse on delete: " + response.getStatus() + " " + response.getResponse());
      if (commit) {
	response = server.commit();
	logger.info("UpdateResponse on commit delete: " + response.getStatus() + " " + response.getResponse());
      }
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new IOException("Error purging records from server", e);
    }
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    override = mode;
  }

  @Override
  public boolean getOverwriteMode() {
    return override;
  }

  @Override
  public OutputStream getOutputStream() {
    return output;
  }

  public String getStorageId() {
    return storageId;
  }

  public void setStorageId(String storageId) {
    this.storageId = storageId;
  }
  
  public StorageStatus getStatus() {
    
    return storageStatus;
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }
}
