package com.indexdata.masterkey.localindices.harvest.storage;

import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;

public class ZooKeeperSolrRecordStorage extends SolrRecordStorage {

  public ZooKeeperSolrRecordStorage(String url, Harvestable harvestable) {
    super(harvestable);
  }

  
  public void init() {
    try {
      Storage storage = null;
      if (harvestable != null) {
        storage = harvestable.getStorage();
      }
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
