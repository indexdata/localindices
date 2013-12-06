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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.job.ConnectorHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordImpl;
import com.indexdata.utils.XmlUtils;

public class HarvestConnectorClient extends AbstractHarvestClient {
  private String sessionId;
  List <Object> linkTokens = new LinkedList<Object>();
  List <String> errors = new ArrayList<String>();
  HttpURLConnectionFactory connectionFactory;

  @Override
  public HarvestConnectorResource getResource() {
    return (HarvestConnectorResource) resource;
  }

  public List<String> getErrors() {
    return errors;
  }
  
  //command pattern starts
  
  public class Unrecoverable extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4160573871208853646L;

    public Unrecoverable(Throwable cause) {
      super(cause);
    }
    
  }
  
  abstract class RetryInvoker {
    final int maxRetries;
    Exception finalException;

    public RetryInvoker(int maxRetries) {
      this.maxRetries = maxRetries;
    }
    
    abstract void onInvoke() throws Exception, Unrecoverable;
    
    public abstract String toString(); //force
    
    boolean invoke() {
      int tried = 0;
      while (true) {
        try {
          tried++;
          onInvoke();
          return true;
        } catch (Unrecoverable ue) {
          logger.warn("Invoking task '"+toString()+"' failed with unretriable error, giving up task");
          finalException = ue;
          return false;
        } catch (StopException e) {
          logger.info("Received Stop Exception. Reason: " + e.getMessage());
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
  
  public class NotInitialized extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -882116435061360920L;

    public NotInitialized(String message) {
      super(message);
    }
    
  }
  
  public abstract class InitializeRetryInvoker extends RetryInvoker {

    public InitializeRetryInvoker(int maxRetries) {
      super(maxRetries);
    }
    
    public abstract void onInit() throws Exception;
    
    public abstract void onPerform() throws Exception, NotInitialized;
    
    public abstract void onError();
    
    @Override
    public void onInvoke() throws Exception {
      try {
        onPerform();
      } catch (NotInitialized ni) {
        logger.warn("Engine session dead, '"+ni.getMessage()+"' trying to initialize..");
        try {
          onInit();
          onPerform();
        } catch (NotInitialized ni2) {
          logger.warn("Initializing session failed.");
          logger.debug("Reason:", ni2);
          onError();
          throw new Unrecoverable(ni2);
        }
      } catch (Exception e) { //everything but non-initialized
        onError();
        throw e;
      }
    }
  }
  
  //command pattern ends, implementation starts
  
  abstract class HarvestInvoker extends InitializeRetryInvoker {

    public HarvestInvoker(int maxRetries) {
      super(maxRetries);
    }
    
    @Override
    final public void onInit() throws Exception {
      createSession();
      uploadConnector(getResource().getConnectorUrl());
      init();
    }

    @Override
    public abstract void onPerform() throws Exception, NotInitialized;
    
    @Override
    final public void onError() {
      try {
        logger.debug("Engine log for the failed invocation:\n" + getLog());
      } catch (Exception le) {
        logger.warn("Retrieving engine log failed with: " + le.getMessage());
      }
    }

    @Override
    final public String toString() {
      return "harvest";
    }
    
  }
  
  public HarvestConnectorClient(HarvestConnectorResource resource, 
    ConnectorHarvestJob job,
    Proxy proxy, StorageJobLogger logger, DiskCache diskCache) {
    super(resource, job, proxy, logger, diskCache); 
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
    logger.log(Level.INFO, "Starting - " + getResource());
    createSession();
    uploadConnector(getResource().getConnectorUrl());
    init();
    new HarvestInvoker(getResource().getRetryCount()) {
      @Override
      public void onPerform() throws Exception {
        harvest(getResource().getResumptionToken(), getResource().getFromDate(),
          getResource().getUntilDate());
      }
    }.invokeOrFail();
    while (!job.isKillSent() && !linkTokens.isEmpty()) {
      final Object linkToken = linkTokens.remove(0);
      RetryInvoker invoker = new HarvestInvoker(getResource().getRetryCount()) {
        @Override
        public void onPerform() throws Exception {
          pause();
          harvest(linkToken);
        }
      };
      boolean success = invoker.invoke();
      if (!success) {
        if (getResource().getAllowErrors()) {
          errors.add("link token '"+linkToken+"' failed with '"
            +invoker.finalException.getMessage()+"'");
          if (invoker.finalException instanceof Unrecoverable) {
            logger.warn("Unrecoverable condition met, harvest terminated.");
            throw invoker.finalException;
          }
          continue;
        }
        else throw invoker.finalException;
      }
    }
    if (job.isKillSent()) {
      logger.log(Level.WARN, "Client stopping premature due to kill signal.\n"); 
    }
    else {
      logger.log(Level.INFO, "Finished - " + getResource());
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
      HttpURLConnection cfwsConn = createConnection("load_cf", null);
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
    HttpURLConnection conn = createConnection("run_task_opt/init", null);
    String initData = getResource().getInitData();
    JSONParser parser = new JSONParser();
    JSONObject jsonObj = new JSONObject();
    try {
     if (initData != null && !initData.equals(""))
       jsonObj = (JSONObject) parser.parse(initData);
    } catch (ParseException pe) {
      logger.error("Failed to parse init data");
      throw new Exception("Failed to parse init data", pe);
    }
    addField(jsonObj, "username", getResource().getUsername());
    addField(jsonObj, "password", getResource().getPassword());
    addField(jsonObj, "proxy",    getResource().getProxy());
    writeJSON(jsonObj, conn);
    executeConnection(conn);
  }

  @SuppressWarnings("unchecked")
  private void addField(JSONObject jsonObj, String fieldname, String value) {
    if (value != null && !value.equals("")) 
      jsonObj.put(fieldname, value);
  }

  private void createSession() throws Exception {
    sessionId = null;
    HttpURLConnection conn = createConnection(null, "logmodules=runtime&loglevel=INFO");
    executeConnection(conn);
    parseSessionResponse(conn.getInputStream(), conn.getContentLength());
  }

  private HttpURLConnection createConnection(String task, String params) throws Exception {
    String urlString = getResource().getUrl() + 
      (sessionId != null ? "/" + sessionId : "") + 
      (task != null ? "/" + task : "") +
      (params != null ? "?" + params : "");
    URL url = new URL(urlString);
    logger.log(Level.INFO, (task == null ? "Creating new session" : "Running " + task ) + " on " + url);

    HttpURLConnection conn = createConnection(url); 
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    return conn; 
  }
  
  /**
   * HUC is very rigid to use -- 404s will throw FileNotFound when retrieving
   * the response code and so will 5xx.
   * @param conn
   * @return
   * @throws Exception 
   */
  @SuppressWarnings("resource")
  private int executeConnection(HttpURLConnection conn) throws Exception {
    int resp = conn.getResponseCode();
    InputStream is = resp >= 400 ? conn.getErrorStream() : conn.getInputStream();
    if (resp >= 400) {
      StringBuilder sb = new StringBuilder();
      read(is, sb, "UTF-8", " ");
      String content = sb.toString();
      if (resp == 400 && content.equals("no such session")) //dead session
        throw new NotInitialized("400 - no such session");
      throw new Exception("Response code "+resp+" - '"+content+"'");
    }
    return resp;
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
    HttpURLConnection conn = createConnection("run_task/harvest", null);
    writeJSON(request, conn);
    executeConnection(conn);
    parseHarvestResponse(conn.getInputStream(), conn.getContentLength());
}
	
  private String getLog() throws Exception {
    HttpURLConnection conn = createConnection("log", "clear=1");
    JSONObject jsonObj = new JSONObject();
    writeJSON(jsonObj, conn);
    executeConnection(conn);
    return read(conn);
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
	  pzRecord.setId(getResource().getId().toString() + "-" + firstValue);
	}
      }
    }
    pzRecord.setDatabase(getResource().getId().toString());
    job.getStorage().add(pzRecord);
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
        getResource().setResumptionToken("{\"start\": \""+(String) startTokenItem+"\"}");
      } else if (startTokenItem instanceof List) {
        getResource().setResumptionToken("{\"start\": "+JSONArray.toJSONString((List)startTokenItem)+"}");
      } else if (startTokenItem instanceof Map) {
        getResource().setResumptionToken("{\"start\": "+JSONObject.toJSONString((Map)startTokenItem)+"}");
      }
    }
  }

  private void pause() throws InterruptedException {
    Long sleep = getResource().getSleep();
    if (sleep != null) {
      logger.debug("Sleeping " + sleep + " before next request...");
      Thread.sleep(getResource().getSleep());
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
      @SuppressWarnings("rawtypes")
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
  
  private String read(HttpURLConnection conn) throws IOException {
    InputStream in = conn.getInputStream();
    StringBuilder sb = new StringBuilder();
    read(in, sb, "UTF-8", "\n");
    return sb.toString();
  }
  
  private void read(InputStream in, StringBuilder sb, String encoding, String sepString) throws IOException {
    InputStreamReader is = new InputStreamReader(in, encoding);
    BufferedReader br = new BufferedReader(is);
    String line = null;
    String sep = "";
    while ((line = br.readLine()) != null) {
      sb.append(sep).append(line);
      sep = sepString;
    }
  }
  
  
  @SuppressWarnings("unused")
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
