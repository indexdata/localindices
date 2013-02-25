package com.indexdata.masterkey.localindices.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;

public class HarvestConnectorClient implements HarvestClient {
  Logger logger = Logger.getLogger(getClass());
  HarvestConnectorResource resource;
  String sessionId;
  Proxy proxy = null; 
  
  public class HarvestToken  {
    public String resumptionToken; 
    public String startDate;
    public String endDate; 
    //  Other values? 
    HarvestToken(String token, String start, String end) {
      resumptionToken = token; 
      startDate = start;
      endDate = end;
    }

  }
  
  List <HarvestToken> jobs = new LinkedList<HarvestToken>();
  

  public HarvestConnectorClient(HarvestConnectorResource resource, Proxy proxy) {
    this.resource = resource; 
    this.proxy = proxy;
  }

  ContainerFactory containerFactory = new ContainerFactory(){
    @SuppressWarnings("rawtypes")
    public List creatArrayContainer() {
      return new LinkedList();
    }

    @SuppressWarnings("rawtypes")
    public Map createObjectContainer() {
      return new LinkedHashMap();
    }
                        
  };
  private int recordCount = 0;

  
  @Override
  public int download(URL url) throws Exception 
  {
    createSession(resource.getUrl());
    // TODO fetchConnector(cfrepo, resource); 
    logger.log(Level.INFO, "Starting - " + resource);

    uploadConnector(resource.getConnector());
    init();

    jobs.add(new HarvestToken(resource.getResumptionToken(), resource.getStartDate(), resource.getEndDate()));
    while (!jobs.isEmpty()) 
      harvest(jobs.get(0));
    
    System.out.println("Log: " + getLog()); 
    logger.log(Level.INFO, "Finished - " + resource);
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
      throw new Exception("Unable to upload connector. Response Code: " + rc);
    }
  }
  

  private void init() throws Exception {
    HttpURLConnection conn = createConnectionJSON("run_task_opt/init");
    String initData = resource.getInitData();
    JSONParser parser = new JSONParser();
    JSONObject jsonObj = new JSONObject();
    try {
     if (initData != null && !initData.equals(""))
       jsonObj = (JSONObject) parser.parse(initData);
    
    } catch (ParseException pe) {
      logger.error("Failed to parse init data");
      throw new Exception("Failed to parse init data", pe);
    }
    addField(jsonObj, "username", resource.getUsername());
    addField(jsonObj, "password", resource.getPassword());
    addField(jsonObj, "proxy",    resource.getProxy());
    
    if ( initData != null && !"".equals(initData)) {
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

  @SuppressWarnings("unchecked")
  private void addField(JSONObject jsonObj, String fieldname, String value) {
    if (value != null && !value.equals("")) 
      jsonObj.put(fieldname, value);
  }

  private void createSession(String url) throws Exception {
    HttpURLConnection conn = createConnectionJSON(null);
    int rc = conn.getResponseCode();
    if (rc == 200) {
      parseSessionResponse(conn.getInputStream(), conn.getContentLength());
      logger.info("New Session (" + sessionId + ") created");
    }
    else 
      System.err.println("Error creating session: " + rc);
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

  private void harvest(HarvestToken parameters) throws Exception 
  {
      HttpURLConnection conn = createConnectionJSON("run_task/harvest");
      JSONObject jsonObj = createHarvestRequest(parameters.resumptionToken, parameters.startDate, parameters.endDate);
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
	System.err.println(getLog()); 
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
      	InputStream in = conn.getInputStream();
      	DataInputStream dataStream = new DataInputStream(in);
      	byte[] b = new byte[contentLength];
      	dataStream.readFully(b);
      	System.out.println("Read vs Content-Length: " + b.length + " " + contentLength);
      	return new String(b,"UTF-8");
    }
    else {
	//System.out.println(getLog()); 
	throw new Exception("Error: ResponseCode:" + responseCode);
    }
  }

  @SuppressWarnings({ "unused", "rawtypes" })
  private void printRecord(JSONObject record) {
    for (Object key: record.keySet()) {
      if (key instanceof String) {
	Object obj = record.get(key); 
	if (obj instanceof List) {
	  for (Object value: (List) obj)
	    System.out.println("<metadata type=\"" + key + "\">" +  value + "</metadata>");
	}
	else 
	  System.out.println("<metadata type=\"" + key + "\">" +  obj + "</metadata>");
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void parseHarvestResponse(InputStream inputStream, int contentLength) throws Exception {
    Reader reader = new InputStreamReader(inputStream);
    JSONParser parser = new JSONParser();
    Object object = parser.parse(reader, containerFactory);
    
    if (object instanceof Map) {
      Map json = (Map) object;
      Object recordsObj = json.get("records");
      if (recordsObj instanceof List) {
	List records = (List) recordsObj;
	int size= records.size();
	System.out.println("<collections size=\"" + size + "\">");
	for (Object recordObj: records) {
	  System.out.println("<record index=\"" + recordCount++  + "\">");
	  if (recordObj instanceof Map) {
	    Map record = (Map) recordObj; 
	    pause();
	    harvestDetails(record);
	    JSONObject obj = new JSONObject();
	    obj.putAll(record);
	    System.out.println(obj.toJSONString());
	    //job.store(record);
	  }
	  System.out.println("</record>");
	}
	System.out.println("</collections>");
      }
      Object resumptionTokensArray = json.get("resumptiontokens");
      if (resumptionTokensArray instanceof List) {
	List resumptionArray = (List) resumptionTokensArray;
	int size= resumptionArray.size();
	System.out.println("<resumptiontokens size=\"" + size + "\">");
	for (Object resumptionTokenObj: resumptionArray) {
	  if (resumptionTokenObj instanceof String) {
	    System.out.println("<resumptiontoken>" + resumptionTokenObj + "</resumptionToken>");
	    pause();
	    add((String) resumptionTokenObj, resource.getStartDate(), resource.getEndDate());
	  }
	}
	System.out.println("</resumptiontokens>");
      }
    }
  }

  private void add(String resumptionTokenObj, String startDate, String endDate) {
    
  }

  private void pause() throws InterruptedException {
    Long sleep = resource.getSleep();
    if (sleep != null) {
      logger.debug("Sleeping " + sleep + " before next harvest");
      Thread.sleep(resource.getSleep());
    }
  }

  @SuppressWarnings("rawtypes")
  private Map parseDetailResponse(InputStream inputStream, int contentLength) throws IOException, ParseException 
  {
    Reader reader = new InputStreamReader(inputStream);
    JSONParser parser = new JSONParser();
    Object object = parser.parse(reader, containerFactory);
    
    if (object instanceof Map) {
      Map json = (Map) object;
      return json; 
    }
    logger.warn("No Map in detailed response");
    return null;
  }

  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void harvestDetails(Map record) throws Exception 
  {
    // printRecord(record);
    Object detailTokenArrayObj = record.get("detailtoken");
    if (detailTokenArrayObj != null && detailTokenArrayObj instanceof List) {
	for (Object detailTokenObj : (List) detailTokenArrayObj) {
	  if (detailTokenObj instanceof String) {
	    HttpURLConnection conn = createConnectionJSON("run_task/detail");
	    JSONObject detailRequest = createDetailRequest((String) detailTokenObj);
	    conn.setDoOutput(true);
	    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
	    out.writeBytes(detailRequest.toJSONString());
	    out.flush();
	    int rc = conn.getResponseCode();
	    if (rc == 200)  {
	      Map detailedObj = parseDetailResponse(conn.getInputStream(), conn.getContentLength());
	      record.putAll(detailedObj);
	    } else {
	      	System.err.println("Error getting detail for " + (String) detailTokenObj + ". Response Code: " + rc + ":\n" + getLog());
	    }
	    
	  }
	}
    }
    else 
      logger.info("Not detail record for" + record.get("url"));
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

  @SuppressWarnings("rawtypes")
  private String parseSessionResponse(InputStream in, long contentLength) throws ParseException, IOException {
    JSONParser parser = new JSONParser();
    sessionId = null;
    Object object = parser.parse(new InputStreamReader(in));
    if (object instanceof Map) {
      Map json = (Map) object;
      Object id = json.get("id");
      if (id instanceof Number) {
	Number number = (Number) id;
	sessionId = number.toString();
      }
    }
    return sessionId;
  }
}
