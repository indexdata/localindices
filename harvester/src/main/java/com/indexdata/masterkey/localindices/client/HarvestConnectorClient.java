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
import com.indexdata.utils.XmlUtils;
import java.net.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.w3c.dom.Document;

public class HarvestConnectorClient implements HarvestClient {
  private StorageJobLogger logger; 
  private HarvestConnectorResource resource;
  private String sessionId;
  private Proxy proxy = null; 
  private RecordHarvestJob job; 
  private int recordCount = 0;
  RecordStorage storage; 
  List <String> linkTokens = new LinkedList<String>();

  public void setHarvestJob(RecordHarvestJob parent) {
    job = parent;
    logger = job.getLogger();
  }
  
  public HarvestConnectorClient(HarvestConnectorResource resource, Proxy proxy) {
    this.resource = resource; 
    this.proxy = proxy;
  }

  ContainerFactory containerFactory = new ContainerFactory() {
    @SuppressWarnings("rawtypes")
    public List creatArrayContainer() {
      return new LinkedList();
    }

    @SuppressWarnings("rawtypes")
    public Map createObjectContainer() {
      return new LinkedHashMap();
    }
                        
  };
  
  @Override
  public int download(URL url) throws Exception 
  {
    storage = job.getStorage();
    createSession(resource.getUrl());
    // TODO fetchConnector(cfrepo, resource); 
    logger.log(Level.INFO, "Starting - " + resource);
    uploadConnector(resource.getConnectorUrl());
    init();
    harvest(resource.getResumptionToken(), resource.getFromDate(), resource.getUntilDate());
    while (!job.isKillSent() && !linkTokens.isEmpty()) {
      pause();
      harvest(linkTokens.remove(0));
    }
    if (job.isKillSent()) {
      logger.log(Level.WARN, "Client stopping premature due to kill signal.\n"); 
    }
    else {
      logger.log(Level.INFO, "Engine Log:\n" + getLog()); 
      logger.log(Level.INFO, "Finished - " + resource);
    }
    return 0;
}
  
  private void uploadConnector(String connUrl) throws Exception {
    //fetch from the repo
    HttpMethod hm = null;
    try { 
      logger.info("Fetching harvesting connector from "+connUrl);
      HttpClient hc = new HttpClient();
      URI connUri = new URI(connUrl);
      if (connUri.getUserInfo() != null) {
        hc.getState().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(connUri.getUserInfo()));
      }
      hm = new GetMethod(connUrl);
      int res = hc.executeMethod(hm);
      if (res != 200) {
        throw new Exception("Fetching connector from the repo failed - status "+res);
      }
      Document connector = XmlUtils.parse(hm.getResponseBodyAsStream());
      HttpURLConnection cfwsConn = createConnectionJSON("load_cf");
      cfwsConn.setDoOutput(true);
      XmlUtils.serialize(connector, cfwsConn.getOutputStream());
      int rc = cfwsConn.getResponseCode();
      if (rc != 200) {
        throw new Exception("Unable to post connector to CFWS - status " + rc);
      }
    } catch (URISyntaxException ue) {
      throw new Exception("Fetching connector from the repo failed", ue);
    } finally {
      if (hm != null) hm.releaseConnection();
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
  
  private void harvest(String linkToken) throws Exception {
    JSONObject jsonObj = createHarvestRequest(linkToken);    
    if (jsonObj == null) 
      throw new Exception("Error creating JSON harvest request object");
    harvest(jsonObj);
  }
  
  private void harvest(String startToken, Date startDate, Date endDate) throws Exception {
    JSONObject jsonObj = createHarvestRequest(startToken, 
      startDate, endDate);    
    if (jsonObj == null) 
      throw new Exception("Error creating JSON harvest request object");
    harvest(jsonObj);
  }

  private void harvest(JSONObject request) throws Exception {
      HttpURLConnection conn = createConnectionJSON("run_task/harvest");
      String postdata = request.toJSONString();
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
  public void storeRecord(JSONObject record) throws IOException {
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
    //parse results
    Object object = parser.parse(reader, containerFactory);
    if (object instanceof Map) {
      Map json = (Map) object;
      Object recordsObj = json.get("results");
      if (recordsObj instanceof List) {
	List records = (List) recordsObj;
	for (Object recordObj: records) {
	  if (recordObj instanceof Map) {
	    Map record = (Map) recordObj;
            JSONObject recordJSON = new JSONObject();
            recordJSON.putAll(record);
	    storeRecord(recordJSON);
	  }
	}
      }
      //parse links
      Object linkTokensItem = json.get("links");
      if (linkTokensItem instanceof List) {
	List linkTokens = (List) linkTokensItem;
	for (Object linkTokenItem: linkTokens) {
	  if (linkTokenItem instanceof String) {
            this.linkTokens.add((String) linkTokenItem);
	  } else if (linkTokenItem instanceof List) {
            this.linkTokens.add(JSONArray.toJSONString((List)linkTokenItem));
          } else if (linkTokenItem instanceof Map) {
            this.linkTokens.add(JSONObject.toJSONString((Map)linkTokenItem));
          }
	  logger.debug("link tokens received: " + linkTokenItem);
	}
      }
      //parse and remember starttoken
      Object startTokenItem = json.get("start");
      if (startTokenItem instanceof String) {
        resource.setResumptionToken((String) startTokenItem);
      } else if (startTokenItem instanceof List) {
        resource.setResumptionToken(JSONArray.toJSONString((List)startTokenItem));
      } else if (startTokenItem instanceof Map) {
        resource.setResumptionToken(JSONObject.toJSONString((Map)startTokenItem));
      }
    }
  }

  private void pause() throws InterruptedException {
    Long sleep = resource.getSleep();
    if (sleep != null) {
      logger.debug("Sleeping " + sleep + " before next harvest");
      Thread.sleep(resource.getSleep());
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
  public JSONObject createHarvestRequest(String linkToken) throws ParseException 
  {
    JSONObject request = new JSONObject();
    JSONParser p = new JSONParser();
    Object token = p.parse(linkToken, containerFactory);
    if (linkToken != null)
      request.put("link", token);
    return request;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject createHarvestRequest(String startToken, Date startDate, Date endDate) 
  {
    JSONObject request = new JSONObject();
    if (startToken != null)
      request.put("start", startToken);
    if (startDate != null) 
      request.put("startDate", formatDate(startDate));
    if (endDate != null)
      request.put("endDate", formatDate(endDate));
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
