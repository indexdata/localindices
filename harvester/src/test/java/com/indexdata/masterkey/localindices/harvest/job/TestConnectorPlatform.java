package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Stack;

import junit.framework.TestCase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;

public class TestConnectorPlatform extends TestCase {
  String cfServer = "http://usi03.indexdata.com:9010/connector";
  //String cfServer = "http://satay.index:9000/connector";
  String session = "{\"id\":3}";
  HarvestConnectorResource resource; 
  
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

  public void testDownload_indexdata() throws ParseException, IOException {
    HarvestConnectorResource resource = new HarvestConnectorResource();
    resource.setUrl(cfServer);
    resource.setInitData("{}");
    resource.setConnector(createConnectorFromResource("resources/id.cf"));
    HarvestConnectorClient client = new HarvestConnectorClient(resource);
    try {
      client.download(null);
    } catch (Exception exp) {
      System.out.println(exp.getMessage()); 
      exp.printStackTrace();
    }
  }

  public void testDownload_sheetmusicconsortium() throws ParseException, IOException {
    HarvestConnectorResource resource = new HarvestConnectorResource();
    resource.setUrl(cfServer);
    resource.setInitData("{}");
    resource.setSleep(new Long(500));
    resource.setConnector(createConnectorFromResource("resources/sheetmusicconsortium.cf"));
    HarvestConnectorClient client = new HarvestConnectorClient(resource);
    try {
      client.download(null);
    } catch (Exception exp) {
      System.out.println(exp.getMessage()); 
      exp.printStackTrace();
    }
  }

  @SuppressWarnings("unused")
  private void printRecord(JSONObject record) {
    for (Object key: record.keySet()) {
      if (key instanceof String) {
	Object obj = record.get(key); 
	if (obj instanceof JSONArray) {
	  for (Object value: (JSONArray) obj)
	    System.out.println("<metadata type=\"" + key + "\">" +  value + "</metadata>");
	}
	else 
	  System.out.println("<metadata type=\"" + key + "\">" +  obj + "</metadata>");
      }
    }
  }

  private JSONObject testCreateHarvestRequest(String resumptiontoken, String startDate, String endDate) {
    HarvestConnectorClient client = new HarvestConnectorClient(resource); 
    return client.createHarvestRequest(resumptiontoken, startDate, endDate);
  }
    
  class PzHandler implements ContentHandler {
    private Object value;
    private boolean end = false;
    private String key;
    private boolean inRecords;
    private boolean inRecord;
    private Stack<String> stack = new Stack<String>();
                    
    public Object getValue(){
      return value;
    }
          
    public boolean isEnd(){
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
      if(key != null && inRecord){
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
      }
      else if (inRecord) {
	
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
      String in= stack.pop();
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
