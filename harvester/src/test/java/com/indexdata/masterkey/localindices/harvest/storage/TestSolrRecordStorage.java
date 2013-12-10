package com.indexdata.masterkey.localindices.harvest.storage;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.harvest.job.ConsoleStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
public class TestSolrRecordStorage extends TestCase {
  String solrUrl = "http://localhost:8585/solr/";
  String zkSolrUrl = "opencontent-solr.index:9983";
  Harvestable harvestable = new DummyXmlBulkResource(solrUrl);
  
  // Console Storage
  StorageJobLogger logger = new ConsoleStorageJobLogger(RecordStorage.class, harvestable);
  
  // Solr Storage
  //HarvestStorage storage = new SolrRecordStorage(harvestable);

  public TestSolrRecordStorage() {

  }

  /*
  public void testConnectCloudSolrServerServer() throws IOException {
    CloudSolrServer server = new CloudSolrServer(zkSolrUrl);
    server.setZkClientTimeout(100000); // socket read timeout
    server.setZkConnectTimeout(100000);
    server.setDefaultCollection("collection1");
    server.connect();
    ZkStateReader stateReader = server.getZkStateReader();
    assertTrue("No StateReader received", stateReader != null);
    ClusterState state = stateReader.getClusterState();
    assertTrue("No state received", state != null);
    Set<String> nodes = state.getLiveNodes();
    logger.debug("Connected to cluster with following live nodes: " + StringUtils.join(nodes.toArray(), ","));
  }
  */
  public void testMasterSlaveUrl()
  {
    SolrStorageEntity entity = new SolrStorageEntity();
    String master = "http://master/solr";
    String slave = "http://slave/solr-slave";
    entity.setUrl(master);
    assertTrue("Master not correct", master.equals(entity.getIndexingUrl()));
    assertTrue("Slave not correct", master.equals(entity.getSearchUrl()));
    entity.setUrl(master + ";" + slave);
    assertTrue("Master not correct", master.equals(entity.getIndexingUrl()));
    assertTrue("Slave not correct", slave.equals(entity.getSearchUrl()));
  }
}

