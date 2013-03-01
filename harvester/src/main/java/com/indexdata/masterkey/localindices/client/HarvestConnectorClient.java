package com.indexdata.masterkey.localindices.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public class HarvestConnectorClient implements HarvestClient {
  private StorageJobLogger logger; 
  private HarvestConnectorResource resource;
  private String sessionId;
  private Proxy proxy = null; 
  private RecordHarvestJob job; 
  RecordStorage storage; 

  public class HarvestToken  {
    public String resumptionToken; 
    public Date startDate;
    public Date endDate; 
    //  Other values? 
    HarvestToken(String token, Date start, Date end) {
      resumptionToken = token; 
      startDate = start;
      endDate = end;
    }

  }
  
  List <HarvestToken> jobs = new LinkedList<HarvestToken>();

  public void setHarvestJob(RecordHarvestJob parent) {
    job = parent;
    logger = job.getLogger();
  }


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
    storage = job.getStorage();
    createSession(resource.getUrl());
    // TODO fetchConnector(cfrepo, resource); 
    logger.log(Level.INFO, "Starting - " + resource);

    uploadConnector(resource.getConnector());
    init();
    add(resource.getResumptionToken(), resource.getFromDate(), resource.getUntilDate());
    while (!jobs.isEmpty()) { 
      harvest(jobs.remove(0));
    }
    logger.log(Level.INFO, "Engine Log:\n" + getLog()); 
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
      return ; 
    }
    String error = "Error creating session on " + url + ". Return code: " + rc;
    logger.error(error);
    throw new Exception(error);
  }

  private HttpURLConnection createConnectionRaw(String task) throws Exception {
    HttpURLConnection conn = null; 
    String urlString = resource.getUrl() + (sessionId != null ? "/" + sessionId : "") + (task != null ? "/" + task : "");
    URL url = new URL(urlString);
    logger.log(Level.INFO, (task == null ? "Creating new session" : "Running " + task ) + " on " + url);

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
  
  String currentDateFormat = "yyyy-MM-dd";
  private String formatDate(Date date) {
    if (date == null)
      return null;
    return new SimpleDateFormat(currentDateFormat).format(date);
  }


  private void harvest(HarvestToken parameters) throws Exception 
  {
      HttpURLConnection conn = createConnectionJSON("run_task/harvest");
      JSONObject jsonObj = createHarvestRequest(parameters.resumptionToken, formatDate(parameters.startDate), formatDate(parameters.endDate));
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
	logger.error("Failed to harvest. Engine Log:" + getLog()); 
	throw new Exception("Error: ResponseCode:" + responseCode);
      }
}
	
  private String getLog() throws Exception {
    HttpURLConnection conn = createConnectionJSON("log");
    JSONObject jsonObj = new JSONObject();
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
      	InputStream in = conn.getInputStream();
      	DataInputStream dataStream = new DataInputStream(in);
      	byte[] b = new byte[contentLength];
      	dataStream.readFully(b);
      	return new String(b,"UTF-8");
    }
    else {
	throw new Exception("Error: ResponseCode:" + responseCode);
    }
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void postRecord(JSONObject record) throws IOException {
    Map<String, Collection<Serializable>> mapValues = new LinkedHashMap<String, Collection<Serializable>>();
    RecordImpl pzRecord = new RecordImpl(mapValues);
    recordCount++;
    for (Object keyObj: record.keySet()) {
      if (keyObj instanceof String) {
	String key = (String) keyObj;
	Object obj = record.get(key); 

	Collection<Serializable> collection = new LinkedList<Serializable>();
	if (obj instanceof String) {
	  collection.add((String) obj);
	  mapValues.put(key, collection);
	} else if (obj instanceof Collection<?>) {
	  collection = (Collection<Serializable>) obj;
	  /*
	  for (Serializable value: (List<Serializable>) obj) {
	    collection.add(value);
	  }
	  */
	  mapValues.put(key, collection);
	} else if (obj instanceof Map) {
	  //TODO flatten objects correctly
	  JSONObject jsonObject = new JSONObject();
	  jsonObject.putAll((Map) obj);
	  collection.add(jsonObject.toJSONString());
	  mapValues.put(key, collection);
	} else {
	  logger.warn("Unhandled value type: " + keyObj.getClass().toString());
	}
	/* Generate key based on database and (id or url) 
	 * Id take precedence over url */ 
	if ("id".equals(key) || ("url".equals(key) && pzRecord.getId() == null)) {
	  String firstValue = (String) collection.iterator().next();
	  pzRecord.setId(resource.getId().toString() + "-" + firstValue);
	}
      }
    }
    pzRecord.setDatabase(resource.getId().toString());
    storage.add(pzRecord);
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
	for (Object recordObj: records) {
	  if (recordObj instanceof Map) {
	    Map record = (Map) recordObj; 
	    pause();
	    harvestDetails(record);
	    JSONObject obj = new JSONObject();
	    obj.putAll(record);
	    postRecord(obj);
	    //job.store(record);
	  }
	}
      }
      Object resumptionTokensArray = json.get("resumptiontokens");
      if (resumptionTokensArray instanceof List) {
	List resumptionArray = (List) resumptionTokensArray;
	for (Object resumptionTokenObj: resumptionArray) {
	  if (resumptionTokenObj instanceof String) {
	    logger.debug("resumptiontoken received: " + resumptionTokenObj);
	    pause();
	    add((String) resumptionTokenObj, resource.getFromDate(), resource.getUntilDate());
	  }
	}
      }
    }
  }

  private void add(String resumptionTokenObj, Date startDate, Date endDate) {
    jobs.add(new HarvestToken(resumptionTokenObj, startDate, endDate)); 
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
	      	logger.warn("Error getting detail for " + (String) detailTokenObj + ". Response Code: " + rc + ":\n" + getLog());
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
