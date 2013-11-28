package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.ZkStateReader;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.Harvestable;
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

  public void testConnectCloudSolrServerServer() throws IOException {
    CloudSolrServer server = new CloudSolrServer(zkSolrUrl);
    server.setZkClientTimeout(100000); // socket read timeout
    server.setZkConnectTimeout(100000);
    //server.setDefaultCollection("Collection1");
    server.connect();
    ZkStateReader stateReader = server.getZkStateReader();
    assertTrue("No StateReader received", stateReader != null);
    ClusterState state = stateReader.getClusterState();
    assertTrue("No state received", state != null);
    Set<String> nodes = state.getLiveNodes();
    logger.debug("Connected to cluster with following live nodes: " + StringUtils.join(nodes.toArray(), ","));
  }
}
