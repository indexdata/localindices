package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.noggit.JSONWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import junit.framework.TestCase;

public class TestConnectorPlatform extends TestCase {
  
  String session = "{\"id\":3}";
  
  public void testExampleJsonParsing() throws ParseException {
    JSONParser parser = new JSONParser();
    Object object = parser.parse(session);
    if (object instanceof JSONObject) {
      JSONObject json = (JSONObject) object;
      Object id = json.get("id");
      if (id instanceof Number) {
	Number number = (Number) id;
	System.out.println("Number: " + number);
      }
    }
  }

  public void testHarvestParsing() throws ParseException, IOException {
    InputStream in = getClass().getResourceAsStream("resources/harvest.json");
    Reader reader = new InputStreamReader(in);
    JSONParser parser = new JSONParser();
    Object object = parser.parse(reader); // TODO add container factory, so we get ordered lists
    
    if (object instanceof JSONObject) {
      JSONObject json = (JSONObject) object;
      Object recordsObj = json.get("records");
      if (recordsObj instanceof JSONArray) {
	JSONArray records = (JSONArray) recordsObj;
	int size= records.size();
	System.out.println("<collections size=\"" + size + "\">");
	for (Object recordObj: records) {
	  System.out.println("<record>");
	  if (recordObj instanceof JSONObject) {
	      printRecord((JSONObject) recordObj);
	  }
	  System.out.println("</record>");
	}
	System.out.println("</collections>");
      }
    }
  }

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

  @SuppressWarnings("unchecked")
  private JSONObject testCreateHarvestRequest(String resumptiontoken, Date startDate, Date endDate) {
    	JSONObject request = new JSONObject();
    	SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    	if (resumptiontoken != null)
    	  request.put("resumptiontoken", resumptiontoken);
    	if (startDate != null) 
    	  request.put("startDate", format.format(startDate));
    	if (endDate != null)
  	  request.put("endDate", format.format(endDate));
    	return request;
  }
  
  public void testHarvestRequest() throws IOException 
  {
    JSONObject request = testCreateHarvestRequest("2012-01-01", new Date(2000, 0, 1), new Date(2012, 11, 31));
    StringWriter out = new StringWriter();
    request.writeJSONString(out);
    String jsonText = out.toString();
    System.out.print(jsonText);
  }
  
  class PzHandler implements ContentHandler {
    private Object value;
    private boolean end = false;
    private String key;
    private boolean inRecords;
                    
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
      if(key != null){
          this.value = value;
          key = null;
      }
      return true;
    }

    public boolean startArray() throws ParseException, IOException {
      return true;
    }

          
    public boolean startObject() throws ParseException, IOException {
      return true;
    }

    public boolean startObjectEntry(String key) throws ParseException, IOException {
      this.key = key;
      if (key.equals("records"))
	inRecords = true;
      return true;
    }
          
    public boolean endArray() throws ParseException, IOException {
      return false;
    }

    public boolean endObject() throws ParseException, IOException {
      return true;
    }

    public boolean endObjectEntry() throws ParseException, IOException {
      return true;
    }
  }

  
}
