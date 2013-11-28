package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Set;

import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.ZkStateReader;

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
      server.setZkConnectTimeout(100000);
      this.server = server;
      // TODO make configurable here and in lui-solr packages
      server.setDefaultCollection("collection1");
      server.connect();
      ZkStateReader stateReader = server.getZkStateReader();
      ClusterState state = stateReader.getClusterState();
      Set<String> nodes = state.getLiveNodes();
      logger.debug("Connected to cluster with following live nodes: " + nodes.size());
      storageStatus = new SolrStorageStatus(server, databaseField + harvestable.getId());
    } catch (Exception e) {
      e.printStackTrace();
      // throw new RuntimeException("Unable to init Solr Server: " + e.getMessage(), e);
    }
  }
}
