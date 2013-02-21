package com.indexdata.masterkey.localindices.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;

public class HarvestConnectorClient implements HarvestClient {
  Logger logger = Logger.getLogger(getClass());
  HarvestConnectorResource resource;
  String sessionId;
  Proxy proxy = null; 
  
  public HarvestConnectorClient(HarvestConnectorResource resource) {
    this.resource = resource; 
  }

  @Override
  public int download(URL url) throws Exception 
  {

    createSession(resource.getUrl());
    // fetchConnector(cfrepo, resource); 
    uploadConnector(resource.getConnector());
    System.out.println("Log: " + getLog()); 
    init(); 
    System.out.println("Log: " + getLog()); 
    harvest(resource.getResumptionToken(), resource.getStartDate(), resource.getEndDate());
    System.out.println("Log: " + getLog()); 
    logger.log(Level.INFO, "Finished - " + url.toString());
    return 0;
}
  
  private void uploadConnector(String connector) throws Exception {
    HttpURLConnection conn = createConnectionJSON("load_cf");
    conn.setDoOutput(true);
    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
    out.writeBytes(connector);
    out.flush();
    out.close();
    int rc = conn.getResponseCode();
    if (rc != 200) {
      throw new Exception("Unable to upload connector. Response Code: " + 200);
    }
  }
  

  private void init() throws Exception {
    HttpURLConnection conn = createConnectionJSON("run_task_opt/init");
    String initData = resource.getInitData();
    if ( initData != null) {
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Length", "" + initData.length());
      DataOutputStream out = new DataOutputStream(conn.getOutputStream());
      out.writeBytes(initData);
      out.flush();
      out.close();
    }
    int rc = conn.getResponseCode();
    if (rc == 200) {
      return ;
    }
    String error = "Unable to do init request. Response code: " + rc ;
    logger.warn(error);
    throw new Exception(error); 
  }

  private void createSession(String url) throws Exception {
    HttpURLConnection conn = createConnectionJSON(null);
    int rc = conn.getResponseCode();
    if (rc == 200) {
      parseSessionResponse(conn.getInputStream(), conn.getContentLength());
      logger.info("New Session (" + sessionId + ") created");
    }
  }

  private HttpURLConnection createConnectionRaw(String task) throws Exception {
    HttpURLConnection conn = null; 
    String urlString = resource.getUrl() + (sessionId != null ? "/" + sessionId : "") + (task != null ? "/" + task : "");
    URL url = new URL(urlString);
    logger.log(Level.INFO, "Starting " + task + ": " + url.toString());

    if (proxy != null)
      conn = (HttpURLConnection) url.openConnection(proxy);
    else
      conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    return conn; 
  }

  private HttpURLConnection createConnectionJSON(String task) throws Exception {
    HttpURLConnection conn = createConnectionRaw(task); 
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestMethod("POST");

    return conn; 
  }

  private void harvest(String resumptiontoken, String startDate, String endDate) throws Exception 
  {
      HttpURLConnection conn = createConnectionJSON("run_task/harvest");
      JSONObject jsonObj = createHarvestRequest(resumptiontoken, startDate, endDate);
      if (jsonObj == null) 
	throw new Exception("Error creating JSON harvest request object");
      String postdata = jsonObj.toJSONString();
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Length", "" + postdata.length());
      OutputStream output = conn.getOutputStream();
      DataOutputStream  data = new DataOutputStream(output);
      data.writeBytes(postdata);
      data.flush();
      data.close();
      
      int responseCode = conn.getResponseCode();
      int contentLength = conn.getContentLength();
      // String contentType = conn.getContentType();
      if (responseCode == 200) {
	  parseHarvestResponse(conn.getInputStream(), contentLength);
      }
      else {
	System.out.println(getLog()); 
	throw new Exception("Error: ResponseCode:" + responseCode);
      }
}
	
  private String getLog() throws Exception {
    HttpURLConnection conn = createConnectionJSON("log");
    JSONObject jsonObj = new JSONObject();
    String postdata = jsonObj.toJSONString();
    System.out.print(postdata);
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Length", "" + postdata.length());
    OutputStream output = conn.getOutputStream();
    DataOutputStream  data = new DataOutputStream(output);
    data.writeBytes(postdata);
    data.flush();
    data.close();
    
    int responseCode = conn.getResponseCode();
    int contentLength = conn.getContentLength();
    // String contentType = conn.getContentType();
    if (responseCode == 200) {
      	StringBuffer stringBuffer = new StringBuffer();
      	InputStream in = conn.getInputStream();
      	byte[] b = new byte[4096];
      	while (in.read(b) != -1) {
      	  stringBuffer.append(b);
      	}
      	System.out.print("Read vs Content-Length: " + stringBuffer.length() + " " + contentLength);
      	return stringBuffer.toString();
    }
    else {
	System.out.println(getLog()); 
	throw new Exception("Error: ResponseCode:" + responseCode);
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

  private void parseHarvestResponse(InputStream inputStream, int contentLength) throws Exception {
    Reader reader = new InputStreamReader(inputStream);
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
	    JSONObject record = (JSONObject) recordObj; 
	    harvestDetails(record);
	    //store(record);
	  }
	  System.out.println("</record>");
	}
	System.out.println("</collections>");
      }
      Object resumptionTokensArray = json.get("resumptiontokens");
      if (resumptionTokensArray instanceof JSONArray) {
	JSONArray records = (JSONArray) recordsObj;
	int size= records.size();
	System.out.println("<resumptiontokens size=\"" + size + "\">");
	for (Object resumptionTokenObj: records) {
	  if (resumptionTokenObj instanceof String) {
	    System.out.println("<resumptiontoken>" + resumptionTokenObj + "</resumptionToken>");
	    Thread.sleep(1000);
	    harvest((String) resumptionTokenObj, resource.getStartDate(), resource.getEndDate());
	  }
	  System.out.println("</resumptiontokens>");
	}
      }
    }
  }

  private JSONObject parseDetailResponse(InputStream inputStream, int contentLength) throws IOException, ParseException 
  {
    Reader reader = new InputStreamReader(inputStream);
    JSONParser parser = new JSONParser();
    Object object = parser.parse(reader); // TODO add container factory, so we get ordered lists
    
    if (object instanceof JSONObject) {
      JSONObject json = (JSONObject) object;
      return json; 
    }
    logger.warn("No JSONObject in detailed response");
    return null;
  }

  
  private void harvestDetails(JSONObject record) throws Exception 
  {
    // printRecord(record);
    Object detailTokenArrayObj = record.get("detailtoken");
    if (detailTokenArrayObj != null && detailTokenArrayObj instanceof JSONArray) {
	for (Object detailTokenObj : (JSONArray) detailTokenArrayObj) {
	  if (detailTokenObj instanceof String) {
	    HttpURLConnection conn = createConnectionJSON("run_task/detail");
	    JSONObject detailRequest = createDetailRequest((String) detailTokenObj);
	    conn.setDoOutput(true);
	    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
	    out.writeBytes(detailRequest.toJSONString());
	    out.flush();
	    parseDetailResponse(conn.getInputStream(), conn.getContentLength());
	    //record.putAll(detailedObj);
	  }
	}
    }
  }

  @SuppressWarnings("unchecked")
  public JSONObject createInitRequest(String username, String password, String proxyip) 
  {
    JSONObject request = new JSONObject();
    if (username != null)
      request.put("username", username);
    if (password != null) 
      request.put("password", password);
    if (proxyip != null)
      request.put("proxyip", proxyip);
    return request;
  }

  @SuppressWarnings("unchecked")
  public JSONObject createHarvestRequest(String resumptiontoken, String startDate, String endDate) 
  {
    JSONObject request = new JSONObject();
    if (resumptiontoken != null)
      request.put("resumptiontoken", resumptiontoken);
    if (startDate != null) 
      request.put("startDate", startDate);
    if (endDate != null)
      request.put("endDate", endDate);
    return request;
  }

  @SuppressWarnings("unchecked")
  public JSONObject createDetailRequest(String detailToken)
  {
    JSONObject request = new JSONObject();
    if (detailToken == null)
      throw new RuntimeException("Missing parameter detailtoken");
     request.put("detailtoken", detailToken);
    return request;
  }

  private String parseSessionResponse(InputStream in, long contentLength) throws ParseException, IOException {
    JSONParser parser = new JSONParser();
    sessionId = null;
    Object object = parser.parse(new InputStreamReader(in));
    if (object instanceof JSONObject) {
      JSONObject json = (JSONObject) object;
      Object id = json.get("id");
      if (id instanceof Number) {
	Number number = (Number) id;
	sessionId = number.toString();
      }
    }
    return sessionId;
  }
}
