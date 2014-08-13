package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Set;

import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.ZkStateReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;

public class ZooKeeperSolrRecordStorage extends BulkSolrRecordStorage {

  CloudSolrServer zkServer; 
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
      zkServer = new CloudSolrServer(url);
      zkServer.setZkClientTimeout(100000); // socket read timeout
      zkServer.setZkConnectTimeout(100000);
      zkServer.setDefaultCollection("collection1");
      zkServer.connect();
      this.server = zkServer;
      // TODO make configurable here and in lui-solr packages
      ZkStateReader stateReader = zkServer.getZkStateReader();
      ClusterState state = stateReader.getClusterState();
      Set<String> nodes = state.getLiveNodes();
      logger.debug("Connected to cluster with following live nodes: " + nodes.size());
      storageStatus = new SolrStorageStatus(server, DATABASE_FIELD + harvestable.getId());
    } catch (Exception e) {
      e.printStackTrace();
      // throw new RuntimeException("Unable to init Solr Server: " + e.getMessage(), e);
    }
  }
  
  public void begin() throws IOException {
    zkServer.connect();
    super.begin();
  }

  public void commit() throws IOException {
    super.commit();
    server.shutdown();
  }

  public void rollbacck() throws IOException {
    super.rollback();
    server.shutdown();
  }

}
