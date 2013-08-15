package com.indexdata.masterkey.localindices.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.utils.XmlUtils;

public class HarvestConnectorClient implements HarvestClient {
  private StorageJobLogger logger; 
  private HarvestConnectorResource resource;
  private String sessionId;
  private Proxy proxy = null; 
  private RecordHarvestJob job; 
  RecordStorage storage; 
  List <Object> linkTokens = new LinkedList<Object>();
  List <String> errors = new ArrayList<String>();

  public List<String> getErrors() {
    return errors;
  }
  
  //command pattern starts
  
  abstract class RetryInvoker {
    final int maxRetries;
    Exception finalException;

    public RetryInvoker(int maxRetries) {
      this.maxRetries = maxRetries;
    }
    
    abstract void action() throws Exception;
    public abstract String toString(); //force
    
    boolean invoke() {
      int tried = 0;
      while (true) {
        try {
          tried++;
          action();
          return true;
        } catch (Exception e) {
          boolean retry = tried <= maxRetries;
          logger.warn("Invoking task '"+toString()+"' failed, "
            + (retry ? "retrying "+tried+" of "+maxRetries : "giving up task"));
          logger.debug("Task failure details: ", e);
          if (!retry) {
            finalException = e;
            return false;
          }
        }
      }
    }
    
    void invokeOrFail() throws Exception {
      if (!invoke()) throw finalException;
    }
  }
  
  //command pattern ends

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
    logger.log(Level.INFO, "Starting - " + resource);
    createSession(resource.getUrl());
    uploadConnector(resource.getConnectorUrl());
    init();
    new RetryInvoker(resource.getRetryCount()) {
      @Override
      void action() throws Exception {
        harvest(resource.getResumptionToken(), resource.getFromDate(), resource.getUntilDate());
      }
      @Override
      public String toString() {
        return "harvest";
      }
    }.invokeOrFail();
    while (!job.isKillSent() && !linkTokens.isEmpty()) {
      pause();
      final Object linkToken = linkTokens.remove(0);
      RetryInvoker invoker = new RetryInvoker(resource.getRetryCount()) {
        @Override
        void action() throws Exception {
          harvest(linkToken);
        }
        @Override
        public String toString() {
          return "harvest";
        }
      };
      boolean success = invoker.invoke();
      if (!success) {
        if (resource.getAllowErrors()) {
          errors.add("link token '"+linkToken+"' failed with '"
            +invoker.finalException.getMessage()+"'");
          continue;
        }
        else throw invoker.finalException;
      }
    }
    if (job.isKillSent()) {
      logger.log(Level.WARN, "Client stopping premature due to kill signal.\n"); 
    }
    else {
      logger.log(Level.INFO, getLog()); 
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
    writeJSON(jsonObj, conn);
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
    conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    conn.setRequestMethod("POST");
    return conn; 
  }
  
  String currentDateFormat = "yyyy-MM-dd";
  private String formatDate(Date date) {
    if (date == null)
      return null;
    return new SimpleDateFormat(currentDateFormat).format(date);
  }
  
  private void harvest(Object linkToken) throws Exception {
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
      writeJSON(request, conn);
      int responseCode = conn.getResponseCode();
      int contentLength = conn.getContentLength();
      if (responseCode == 200) {
	  parseHarvestResponse(conn.getInputStream(), contentLength);
      }
      else {
	logger.error("Failed to harvest.\n" + getLog()); 
	throw new Exception("Error: ResponseCode:" + responseCode);
      }
}
	
  private String getLog() throws Exception {
    HttpURLConnection conn = createConnectionJSON("log");
    JSONObject jsonObj = new JSONObject();
    writeJSON(jsonObj, conn);
    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
      int numLines = 10;
      String lines = readNLastLines(conn, numLines);
      StringBuilder sb = new StringBuilder();
      sb.append("Up to ").append(numLines).append(" trailing connector engine log lines shown:\n");
      sb.append(lines);
      sb.append("Engine log ends.\n");
      return sb.toString();
    } else {
	throw new Exception("Error: ResponseCode:" + responseCode);
    }
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void storeRecord(JSONObject record) throws IOException {
    Map<String, Collection<Serializable>> mapValues = new LinkedHashMap<String, Collection<Serializable>>();
    RecordImpl pzRecord = new RecordImpl(mapValues);
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
    Reader reader = new InputStreamReader(inputStream, "UTF-8");
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
          this.linkTokens.add(linkTokenItem); //we don't care what JSON type it is
	  logger.debug("link token received: " + linkTokenItem);
	}
      }
      //parse and remember starttoken
      Object startTokenItem = json.get("start");
      if (startTokenItem instanceof String) {
        resource.setResumptionToken("{\"start\": \""+(String) startTokenItem+"\"}");
      } else if (startTokenItem instanceof List) {
        resource.setResumptionToken("{\"start\": "+JSONArray.toJSONString((List)startTokenItem)+"}");
      } else if (startTokenItem instanceof Map) {
        resource.setResumptionToken("{\"start\": "+JSONObject.toJSONString((Map)startTokenItem)+"}");
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
  public JSONObject createHarvestRequest(Object linkToken) throws ParseException 
  {
    JSONObject request = new JSONObject();
    if (linkToken != null) {
      request.put("link", linkToken);
    }
    return request;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject createHarvestRequest(String startToken, Date startDate, Date endDate) 
    throws ParseException 
  {
    JSONObject request = new JSONObject();
    if (startToken != null && !startToken.isEmpty()) {
      JSONParser p = new JSONParser();
      Map sT = (Map) p.parse(startToken, containerFactory);
      request.putAll(sT);
    }
    if (startDate != null) 
      request.put("startdate", formatDate(startDate));
    if (endDate != null)
      request.put("enddate", formatDate(endDate));
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
    Object object = parser.parse(new InputStreamReader(in, "UTF-8"));
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

  private void writeJSON(JSONObject request, HttpURLConnection conn) throws
    IOException, UnsupportedEncodingException {
    String jsonStr = request.toJSONString();
    byte[] postData = jsonStr.getBytes("UTF-8");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Length", "" + postData.length);
    logger.debug("Task input: "+jsonStr);
    conn.getOutputStream().write(postData);
    conn.getOutputStream().flush();
  }
  
  private String readNLastLines(HttpURLConnection conn, int numLines) throws IOException {
    InputStream in = conn.getInputStream();
    InputStreamReader is = new InputStreamReader(in, "UTF-8");
    BufferedReader br = new BufferedReader(is);
    String read = null;
    String[] lines = new String[numLines];
    int i = 0;
    while ((read = br.readLine()) != null) {
        lines[i] = read;
        i = ++i % numLines;
    }
    StringBuilder sb = new StringBuilder();
    for (int j=i; j<numLines+i; j++) {
      sb.append(lines[j%numLines]).append("\n");
    }
    return sb.toString();
  }
}
