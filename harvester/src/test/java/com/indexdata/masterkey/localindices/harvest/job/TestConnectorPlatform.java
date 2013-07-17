package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Stack;

import org.apache.solr.client.solrj.SolrServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.EmbeddedSolrServerFactory;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SimpleStorageStatus;
import com.indexdata.masterkey.localindices.harvest.storage.SolrServerFactory;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class TestConnectorPlatform extends JobTester {
  String cfServer = "http://usi03.indexdata.com:9010/connector";
  // String cfServer = "http://satay.index:9000/connector";
  String session = "{\"id\":3}";
  String indexdataBlogConnector = "http://idtest:idtest36@cfrepo.indexdata.com/repo.pl/idtest/idblog.6.cf";
  String acceConnectorWithAuth    = "http://idtest:idtest36@cfrepo.indexdata.com/repo.pl/idtest/aace_harvester.7.cf";
  String acceConnectorWithOutAuth = "http://idtest:idtest36@cfrepo.indexdata.com/repo.pl/idtest/aace_harvester.8.cf";
  String solrUrl = "http://localhost:8585/solr/";
  SolrServerFactory factory = new EmbeddedSolrServerFactory(solrUrl);
  SolrServer solrServer = factory.create();

  
  private Transformation createPzTransformation(boolean inParallel) throws IOException {
    String[] resourceSteps = { "resources/pz2-url2id.xsl"};
    return createTransformationFromResources(resourceSteps, inParallel);
  }

  private HarvestConnectorResource createResource(String connector, boolean inParallel, boolean overwrite) throws IOException {
    HarvestConnectorResource resource = new HarvestConnectorResource();
    resource.setId(1l);
    resource.setUrl(cfServer);
    resource.setInitData("{}");
    resource.setConnectorUrl(connector);
    resource.setCurrentStatus("NEW");
    resource.setTransformation(createPzTransformation(inParallel));
    resource.setOverwrite(overwrite);
    return resource;
  }

  /*
  private void testDownload_indexdata() throws ParseException, IOException {
    HarvestConnectorResource resource = createResource("resources/id.cf");
    RecordStorage recordStorage = createStorage(resource);
    HarvestConnectorClient client = new HarvestConnectorClient(resource, null);
    try {
      client.download(null);
    } catch (Exception exp) {
      System.out.println(exp.getMessage());
      exp.printStackTrace();
    }
  }

  private void testDownload_sheetmusicconsortium() throws ParseException, IOException {
    HarvestConnectorResource resource = createResource("resources/sheetmusicconsortium.cf");
    resource.setSleep(new Long(10000));
    HarvestConnectorClient client = new HarvestConnectorClient(resource, null);
    try {
      client.download(null);
    } catch (Exception exp) {
      System.out.println(exp.getMessage());
      exp.printStackTrace();
    }
  }
*/
  private RecordHarvestJob doHarvestJob(RecordStorage recordStorage,
      HarvestConnectorResource resource) throws IOException {
    AbstractRecordHarvestJob job = new ConnectorHarvestJob(resource, null);
    resource.setName(resource.getConnectorUrl());
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  private RecordStorage createStorage(HarvestConnectorResource resource, boolean clean) throws IOException {
    BulkSolrRecordStorage recordStorage = new BulkSolrRecordStorage(solrServer, resource);
    Storage storageEntity = new SolrStorageEntity();
    storageEntity.setId(1l);
    storageEntity.setUrl(solrUrl);
    recordStorage.setWaitSearcher(true);
    if (clean) {
      recordStorage.begin();
      recordStorage.purge(true);
      recordStorage.commit();
    }
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), storageEntity));
    return recordStorage;
  }

  private void testConnectorHarvestJob(String url, boolean overwrite, StorageStatus expected) throws ParseException, IOException, StatusNotImplemented {
    HarvestConnectorResource resource = createResource(url, false, overwrite);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    HarvestStatus status = job.getStatus();
    assertTrue("Harvest Job not finished: " + status, HarvestStatus.FINISHED == status);
    StorageStatus storageStatus = recordStorage.getStatus();
    if (storageStatus != null)
      assertTrue("Result differs from Expected", expected.equals(storageStatus));
    
  }
  
  
  public void testConnectorHarvestJob_id() throws ParseException, IOException, StatusNotImplemented {
    StorageStatus expected  = new SimpleStorageStatus(33, 0, true);
    testConnectorHarvestJob(indexdataBlogConnector, true, expected);
  }
  

  public void testConnectorHarvestJobACCE_overwrite() throws ParseException, IOException, StatusNotImplemented {
    HarvestConnectorResource resource = createResource(acceConnectorWithAuth, false, false);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    HarvestStatus status = job.getStatus();
    assertTrue("Harvest Job not finished: " + status, HarvestStatus.FINISHED == status);
    StorageStatus firstStatus = recordStorage.getStatus();
    resource.setConnectorUrl(acceConnectorWithOutAuth);
    resource.setOverwrite(true);
    job = doHarvestJob(recordStorage, resource);
    assertTrue("Harvest Job not finished: " + status, HarvestStatus.FINISHED == status);
    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue("Result differs from first run", firstStatus.equals(storageStatus));
  }


  @SuppressWarnings("unused")
  private JSONObject testCreateHarvestRequest(HarvestConnectorResource resource, String resumptiontoken, String startDate, String endDate) {
    HarvestConnectorClient client = new HarvestConnectorClient(resource, null);
    return client.createHarvestRequest(resumptiontoken, startDate, endDate);
  }

  public void testPzHandler(JSONObject jsonPz) {
    String jsonText = "{\"first\": 123, \"second\": [{\"k1\":{\"id\":\"id1\"}}, 4, 5, 6, {\"id\": 123}], \"third\": 789, \"id\": null}";
    JSONParser parser = new JSONParser();
    PzHandler finder = new PzHandler();
    try{
      while(!finder.isEnd()){
        parser.parse(jsonText, finder, true);
      }           
    }
    catch(ParseException pe){
      pe.printStackTrace();
    }
  }

  class PzHandler implements ContentHandler {
    private Object value;
    private boolean end = false;
    private String key;
    private boolean inRecords;
    private boolean inRecord;
    private Stack<String> stack = new Stack<String>();

    public Object getValue() {
      return value;
    }

    public boolean isEnd() {
      return end;
    }

    public void startJSON() throws ParseException, IOException {
      end = false;
      inRecords = false;
    }

    public void endJSON() throws ParseException, IOException {
      end = true;
    }

    public boolean primitive(Object value) throws ParseException, IOException {
      if (key != null && inRecord) {
	this.value = value;
	System.out.println("<metadata type=\"" + key + "\">" + value + "</metadata>");
      }
      return true;
    }

    public boolean startArray() throws ParseException, IOException {
      stack.push("inArray");
      return true;
    }

    public boolean startObject() throws ParseException, IOException {
      stack.push("inObject");
      if (inRecords && key.equals("records")) {
	System.out.println("<record>");
	inRecord = true;
      } else if (inRecord) {

      }
      return true;
    }

    public boolean startObjectEntry(String key) throws ParseException, IOException {

      if (key != null)
	stack.push(key);
      this.key = key;
      if ("records".equals(key))
	inRecords = true;
      return true;
    }

    public boolean endArray() throws ParseException, IOException {
      String in = stack.pop();
      assertTrue("Not in array: " + in, "in".equals(in));
      return false;
    }

    public boolean endObject() throws ParseException, IOException {
      String in = stack.pop();
      assertTrue("Not in object: " + in, "inObject".equals(in));
      return true;
    }

    public boolean endObjectEntry() throws ParseException, IOException {
      if (key.equals("record"))
	inRecord = false;
      if (key.equals("records"))
	inRecords = false;
      key = null;
      key = stack.pop();
      return true;
    }
    
  }

}
