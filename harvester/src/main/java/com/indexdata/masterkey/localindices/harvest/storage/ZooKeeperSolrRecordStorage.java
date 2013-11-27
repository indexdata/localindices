package com.indexdata.masterkey.localindices.harvest.storage;

import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;

public class ZooKeeperSolrRecordStorage extends BulkSolrRecordStorage {

  public ZooKeeperSolrRecordStorage() {
  }

  public ZooKeeperSolrRecordStorage(Harvestable harvestable) {
    super(harvestable);
  }

  public ZooKeeperSolrRecordStorage(String url, Harvestable harvestable) {
    super(url, harvestable);
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
      logger = new FileStorageJobLogger(this.getClass(), storage);
      CloudSolrServer server = new CloudSolrServer(url);
      server.setZkClientTimeout(100000); // socket read timeout
      server.setZkClientTimeout(10000);
      this.server = server;
      
      storageStatus = new SolrStorageStatus(server, databaseField + harvestable.getId());
    } catch (Exception e) {
      throw new RuntimeException("Unable to init Solr Server: " + e.getMessage(), e);
    }
  }
}
