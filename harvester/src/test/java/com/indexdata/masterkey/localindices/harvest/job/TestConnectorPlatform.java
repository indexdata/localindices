package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public class TestConnectorPlatform extends JobTester {
  String cfServer = "http://usi03.indexdata.com:9010/connector";
  // String cfServer = "http://satay.index:9000/connector";
  String session = "{\"id\":3}";
  HarvestConnectorResource resource;

  String solrUrl = "http://localhost:8585/solr/";

  private String createConnectorFromResource(String resource) throws IOException {
    InputStream input = getClass().getResourceAsStream(resource);

    assertTrue(input != null);
    byte buf[] = new byte[4096];
    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    int length = 0;
    @SuppressWarnings("unused")
    int total = 0;
    while ((length = input.read(buf)) != -1) {
      byteArray.write(buf, 0, length);
      total += length;
    }
    String connector = byteArray.toString("UTF-8");
    return connector;
  }

  private HarvestConnectorResource createResource(String connector) throws IOException {
    HarvestConnectorResource resource = new HarvestConnectorResource();
    resource.setId(1l);
    resource.setUrl(cfServer);
    resource.setInitData("{}");
    resource.setConnector(createConnectorFromResource(connector));
    resource.setCurrentStatus("NEW");
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
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  private RecordStorage createStorage(HarvestConnectorResource resource) {
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    Storage storageEntity = new SolrStorageEntity();
    storageEntity.setId(1l);
    storageEntity.setUrl(solrUrl);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), storageEntity));
    return recordStorage;
  }

  public void testConnectorHarvestJob() throws ParseException, IOException {
    HarvestConnectorResource resource = createResource("resources/id.cf");
    RecordStorage recordStorage = createStorage(resource);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    HarvestStatus status = job.getStatus();
    if (HarvestStatus.FINISHED == status) {

    } else
      System.out.println("Failed to harvest. Status: " + status.toString());
  }

  @SuppressWarnings("unused")
  private JSONObject testCreateHarvestRequest(String resumptiontoken, String startDate, String endDate) {
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
