/**
 Copyright 2006 OCLC, Online Computer Library Center
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ORG.oclc.oai.harvester2.verb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import ORG.oclc.oai.harvester2.transport.BrokenHttpResponseException;
import ORG.oclc.oai.harvester2.transport.HttpErrorException;
import ORG.oclc.oai.harvester2.transport.ResponseParsingException;

import com.indexdata.io.FailsafeXMLCharacterInputStream;


/**
 * HarvesterVerb is the parent class for each of the OAI verbs.
 * 
 * @author Jefffrey A. Young, OCLC Online Computer Library Center
 */
public abstract class HarvesterVerb {
    protected Logger logger = Logger.getLogger("org.oclc.oai.harvester2");

    private int httpRetries = 2;
    private int httpRetryWait = 600; //secs
    private int httpTimeout = 60000;    //msecs

    /* Primary OAI namespaces */
    public static final String NAMESPACE_V2_0 = "http://www.openarchives.org/OAI/2.0/";
    public static final String SCHEMA_LOCATION_V2_0 = NAMESPACE_V2_0 + " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
    
    public static final String NAMESPACE_V1_1 = "http://www.openarchives.org/OAI/1.1/";
    
    public static final String NAMESPACE_V1_1_GET_RECORD = NAMESPACE_V1_1 + "OAI_GetRecord";
    public static final String SCHEMA_LOCATION_V1_1_GET_RECORD = NAMESPACE_V1_1_GET_RECORD + " http://www.openarchives.org/OAI/1.1/OAI_GetRecord.xsd";
    
    public static final String NAMESPACE_V1_1_IDENTIFY = NAMESPACE_V1_1 + "OAI_Identify";
    public static final String SCHEMA_LOCATION_V1_1_IDENTIFY = NAMESPACE_V1_1_IDENTIFY + " http://www.openarchives.org/OAI/1.1/OAI_Identify.xsd";
    
    public static final String NAMESPACE_V1_1_LIST_IDENTIFIERS = NAMESPACE_V1_1 + "OAI_ListIdentify";
    public static final String SCHEMA_LOCATION_V1_1_LIST_IDENTIFIERS = NAMESPACE_V1_1_LIST_IDENTIFIERS + " http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers.xsd";
    
    public static final String NAMESPACE_V1_1_LIST_METADATA_FORMATS = NAMESPACE_V1_1 + "OAI_ListMetadataFormats";
    public static final String SCHEMA_LOCATION_V1_1_LIST_METADATA_FORMATS = NAMESPACE_V1_1_LIST_METADATA_FORMATS + " http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats.xsd";

    public static final String NAMESPACE_V1_1_LIST_RECORDS = NAMESPACE_V1_1 + "OAI_ListRecords";
    public static final String SCHEMA_LOCATION_V1_1_LIST_RECORDS = NAMESPACE_V1_1_LIST_RECORDS + " http://www.openarchives.org/OAI/1.1/OAI_ListRecords.xsd";
    
    public static final String NAMESPACE_V1_1_LIST_SETS = NAMESPACE_V1_1 + "OAI_ListSets";
    public static final String SCHEMA_LOCATION_V1_1_LIST_SETS = NAMESPACE_V1_1_LIST_SETS + " http://www.openarchives.org/OAI/1.1/OAI_ListSets.xsd";

    private Document doc = null;
    private String schemaLocation = null;
    private String requestURL = null;
    private static HashMap<Thread, DocumentBuilder> builderMap = new HashMap<Thread, DocumentBuilder>();
    public static Element namespaceElement = null;
    private static DocumentBuilderFactory factory = null;
    private boolean useTagSoup = false;
    private static HashMap<Thread, TransformerFactory> transformerFactoryMap = new HashMap<Thread, TransformerFactory>();
    private static HashMap<Thread, XPathFactory> xpathFactoryMap = new HashMap<Thread, XPathFactory>();
    //private static boolean debug = false;
    
    static XPath createXPath() {
      /* create transformer */
      XPathFactory xpathFactory = xpathFactoryMap.get(Thread.currentThread());
      if (xpathFactory == null) {
	xpathFactory = XPathFactory.newInstance();
	xpathFactoryMap.put(Thread.currentThread(), xpathFactory);
      }
      return xpathFactory.newXPath();
    }

    static Transformer createTransformer() {
      /* create transformer */
      TransformerFactory xformFactory = transformerFactoryMap.get(Thread.currentThread());
      if (xformFactory == null) {
	xformFactory = TransformerFactory.newInstance();
	transformerFactoryMap.put(Thread.currentThread(), xformFactory);
      }
      try {
          Transformer transformer = xformFactory.newTransformer();
          transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
          transformer.setOutputProperty(OutputKeys.METHOD, "xml");
          return transformer;
      } catch (TransformerException e) {
          e.printStackTrace();
      }
      return null;
    }
    static {
        try {
          
            /* Load DOM Document */
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Thread t = Thread.currentThread();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builderMap.put(t, builder);
            DOMImplementation impl = builder.getDOMImplementation();
            Document namespaceHolder = impl.createDocument(
                    "http://www.oclc.org/research/software/oai/harvester",
                    "harvester:namespaceHolder", null);
            namespaceElement = namespaceHolder.getDocumentElement();
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:harvester", 
        	"http://www.oclc.org/research/software/oai/harvester");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", 
        	"http://www.w3.org/2001/XMLSchema-instance");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai20", 
        	"http://www.openarchives.org/OAI/2.0/");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_GetRecord",
        	"http://www.openarchives.org/OAI/1.1/OAI_GetRecord");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_Identify",
        	"http://www.openarchives.org/OAI/1.1/OAI_Identify");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_ListIdentifiers",
        	"http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_ListMetadataFormats",
        	"http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_ListRecords",
        	"http://www.openarchives.org/OAI/1.1/OAI_ListRecords");
            namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai11_ListSets",
        	"http://www.openarchives.org/OAI/1.1/OAI_ListSets");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get the OAI response as a DOM object
     * 
     * @return the DOM for the OAI response
     */
    public Document getDocument() {
        return doc;
    }
    
    /**
     * Get the xsi:schemaLocation for the OAI response
     * 
     * @return the xsi:schemaLocation value
     */
    public String getSchemaLocation() {
        return schemaLocation;
    }
    
    /**
     * Get the OAI errors
     * @return a NodeList of /oai:OAI-PMH/oai:error elements
     * @throws TransformerException
     */
    public NodeList getErrors() throws TransformerException 
    {
      	String schemas = getSchemaLocation();
        if (schemas.indexOf(SCHEMA_LOCATION_V2_0) != -1) {
            return getNodeList("/oai20:OAI-PMH/oai20:error");
        }
        else if (schemas.indexOf(SCHEMA_LOCATION_V1_1_LIST_RECORDS) != -1)  {
          return getNodeList("/oai11:OAI-PMH/oai11:error");
          
        }
        else   
          return getNodeList("/OAI-PMH/error");
    }
    
    /**
     * Get the OAI request URL for this response
     * @return the OAI request URL as a String
     */
    public String getRequestURL() {
        return requestURL;
    }
    
    /**
     * Mock object creator (for unit testing purposes)
     */
    public HarvesterVerb() {
    }

    /**
     * Mock object creator (for unit testing purposes)
     */
    public HarvesterVerb(Logger jobLogger) {
    	logger = jobLogger;
    }

    /**
     * Performs the OAI request
     * 
     * @param requestURL
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     * 
     */
    public HarvesterVerb(String requestURL, Proxy proxy, String encodingOverride, Logger jobLogger) throws IOException,
    ParserConfigurationException, TransformerException, ResponseParsingException {
      	logger = jobLogger;
      	harvest(requestURL, proxy, encodingOverride);
    }
    
    /**
     * Preforms the OAI request
     * 
     * @param requestURL
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    public void harvest(String requestURL, Proxy proxy, String encodingOverride) throws 
    		IOException, ParserConfigurationException, 
    		TransformerException, ResponseParsingException {
        this.requestURL = requestURL;
        logger.log(Level.INFO, "Request URL: " + requestURL);
        InputStream in = null;
        URL url = new URL(requestURL);
        HttpURLConnection con = null;
        int responseCode = 0;
        boolean retry;
        int totalRetries = 0;
        do {
            retry = false;
            if (proxy != null)
                con = (HttpURLConnection) url.openConnection(proxy);
            else
                con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "OAIHarvester/2.0");
            con.setRequestProperty("Accept-Encoding", "compress, gzip, identify");
            // TODO Make configurable. 
            con.setConnectTimeout(httpTimeout);
            con.setReadTimeout(httpTimeout);
            try {
                responseCode = con.getResponseCode();
                if (responseCode != 200)
                  logger.log(Level.WARN, "Url: " + url + " ResponseCode: " + responseCode);
                else if (logger.isDebugEnabled()) {
                  logger.log(Level.DEBUG, "Url: " + url + " ResponseCode: " + responseCode);
                }
            } catch (Exception e) {
                // response is broken or a socket timeout occurred, retry nevertheless
                logger.log(Level.WARN, requestURL, e);
                responseCode = -1;
            }
            //for some responses the server will tell us when to retry
            //for others we'll use the defaults
            if (isRetry(responseCode)) {
                long retrySeconds = con.getHeaderFieldInt("Retry-After", -1);
                if (retrySeconds == -1) {
                    //this is because in HTTP date may be already parsed as seconds
                    long now = (new Date()).getTime();
                    long retryDate = con.getHeaderFieldDate("Retry-After", now);
                    retrySeconds = retryDate - now;
                }
                if (retrySeconds == 0) { //header not specified
                    retrySeconds = httpRetryWait;
                    logger.log(Level.INFO,"Server response code '"+responseCode
                            + "' retrying in "+ retrySeconds + " secs");
                } else {
                    logger.log(Level.INFO,"Server response code '"+responseCode
                            + "' Retry-After: "+ retrySeconds);
                }
                if (retrySeconds > 0) {
                    try {
                        Thread.sleep(retrySeconds * 1000);
                    } catch (InterruptedException ex) {
                        throw new IOException("Interrupted while retrying HTTP connection.");
                    }
                }
                retry = ++totalRetries < httpRetries;
            }
        } while (retry);

        if (responseCode == -1) {
            throw new BrokenHttpResponseException("Could not read HTTP response code. Bad URL?");
        }

        //stop for non-recoverable HTTP client/server errrors
        if (responseCode >= 400 && responseCode < 600) {
            String statusMessage = null;
            try {
                statusMessage = con.getResponseMessage();
            } catch (IOException ioe) {
                statusMessage = "<couldn't parse status message>";
            }
            throw new HttpErrorException(responseCode, statusMessage, requestURL);
        }

        String contentEncoding = con.getHeaderField("Content-Encoding");
        if (contentEncoding != null)
          logger.log(Level.INFO, "Content-Encoding: " + contentEncoding);
        if ("compress".equals(contentEncoding)) {
            @SuppressWarnings("resource")
	    ZipInputStream zis = new ZipInputStream(con.getInputStream());
            zis.getNextEntry();
            in = zis;
        } else if ("gzip".equals(contentEncoding)) {
            in = new GZIPInputStream(con.getInputStream());
        } else if ("deflate".equals(contentEncoding)) {
            in = new InflaterInputStream(con.getInputStream());
        } else {
            in = con.getInputStream();
        }
        
        int contentLength = con.getContentLength();
        InputSource data = new InputSource();
        BufferedInputStream bin = null;
        
        if (encodingOverride == null || "".equals(encodingOverride)) {
          bin = new BufferedInputStream(new FailsafeXMLCharacterInputStream(in));
          data.setByteStream(bin);
        }
        else {
          logger.log(Level.INFO, "Enforcing encoding override: '" + encodingOverride + "'");
          bin = new BufferedInputStream(in);
          Reader reader = new InputStreamReader(bin, encodingOverride);
          data.setCharacterStream(reader);
        }
	try {
	  if (bin.markSupported())
	    bin.mark(contentLength);
	  if (isUseTagSoup()) 
	    doc = createTagSoupDocument(data);
	  else 
	    doc = createDocument(data);
	    in.close();
	} catch (SAXException saxe) {
	  in.close();
          bin.reset();
          saxe.printStackTrace();
          throw new ResponseParsingException("Cannot parse response: " + saxe.getMessage(),
                  saxe, bin, requestURL);
	}
	if (logger.isTraceEnabled()) {
          bin.reset();
	  logResponse(doc, bin);
	}
        StringTokenizer tokenizer = new StringTokenizer(
                getSingleString("/*/@xsi:schemaLocation"), " ");
        StringBuffer sb = new StringBuffer();
        while (tokenizer.hasMoreTokens()) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(tokenizer.nextToken());
        }
        this.schemaLocation = sb.toString();
        if ("".equals(schemaLocation)) {
          logger.error("No Schema Location found. Dumping response: "); 
          bin.reset();
          logResponse(doc, bin);
        }
    }

  private void logResponse(Document doc, BufferedInputStream bin) {
    try {
      Transformer transformer = createTransformer();
      transformer.transform(new DOMSource(doc), new StreamResult(System.out));
    } catch (Exception e) {
      logger.error("Failed to trace Response XML document. Dumping buffered response");
      try {
	bin.reset();
	BufferedReader reader = new BufferedReader(new InputStreamReader(bin));
	String line; 
	while ((line = reader.readLine()) != null) {
	  System.out.print("" + line);
	}
      } catch (IOException e1) {
	logger.error("Failed to dump Response");
      }
    }
  }

    private boolean isRetry(int responseCode) {
      return responseCode == -1
       || responseCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT
       || responseCode == HttpURLConnection.HTTP_ENTITY_TOO_LARGE
       || responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR
       || responseCode == HttpURLConnection.HTTP_BAD_GATEWAY
       || responseCode == HttpURLConnection.HTTP_UNAVAILABLE
       || responseCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
    }

    private Document createDocument(InputSource data) throws ParserConfigurationException,
	SAXException, IOException {
      Thread t = Thread.currentThread();
      DocumentBuilder builder = (DocumentBuilder) builderMap.get(t);
      if (builder == null) {
          builder = factory.newDocumentBuilder();
          builderMap.put(t, builder);
      }
      return builder.parse(data);
    }

    private Document createTagSoupDocument(InputSource data) throws SAXException,
	TransformerConfigurationException, TransformerFactoryConfigurationError,
	TransformerException, ParserConfigurationException {
      XMLReader reader = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
      boolean useNamespace = reader.getFeature("http://xml.org/sax/features/namespaces");
      boolean usePrefixes = reader.getFeature("http://xml.org/sax/features/namespace-prefixes");
      reader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      logger.debug("Namespace: " + useNamespace + ". Prefixes: " + usePrefixes);
      Source input = new SAXSource(reader, data);
      Transformer transformer = createTransformer();
      //SAXResult saxResult = new SAXResult(new CleanXMLHandler)
      DOMResult dom = new DOMResult();
      transformer.transform(input, dom);
      
      if (dom.getNode() instanceof Document) { 	
	
	Document doc = (Document) dom.getNode();
	/*
	NodeList list = doc.getElementsByTagName("OAI-PMH");
	Element element = (Element) list.item(0);
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document doc2 = builder.newDocument();
	*/
	return doc;
      }
        
      else 
        logger.error("Not a Document");
      return null;
    }
    
    /**
     * Get the String value for the given XPath location in the response DOM
     * 
     * @param xpath
     * @return a String containing the value of the XPath location.
     * @throws TransformerException
     */
    public String getSingleString(String xpath) throws TransformerException {
        return getSingleString(getDocument(), xpath);
    }
    
  public static String getSingleString(Node node, String xpath) throws TransformerException {
    XPathHelper<String> stringHelper =  new XPathHelper<String>(XPathConstants.STRING, new OaiPmhNamespaceContext());
    try {
      return  stringHelper.evaluate(node, xpath);
    } catch (XPathExpressionException xpee) {
       throw new TransformerException("Failed to evaluate XPath expression: " + xpath, xpee);
     }
  }
    
    /**
     * Get a NodeList containing the nodes in the response DOM for the specified
     * xpath
     * @param xpath
     * @return the NodeList for the xpath into the response DOM
     * @throws TransformerException
     */
  
  public NodeList getNodeList(String xpath) throws TransformerException {
    return getNodeList(getDocument(), xpath);
  }
  
  public NodeList getNodeList(Node node, String xpath) throws TransformerException {
    try {
      XPathHelper<NodeList> xpathHelper = new XPathHelper<NodeList>(XPathConstants.NODESET, new OaiPmhNamespaceContext());
      return xpathHelper.evaluate(node, xpath);
    } catch (XPathExpressionException e) {
      String message = "getNodeList: XPath Expression Exception: ";
      logger.error(message + xpath, e);
      throw new TransformerException(message + xpath, e);
    }
  }
    
    public Boolean getBoolean(String xpath) throws TransformerException {
      return getBoolean(xpath, getDocument());
    }

    public Boolean getBoolean(String xpath, Node node) throws TransformerException {
      try {
	XPathHelper<Boolean> xpathHelper = new XPathHelper<Boolean>(XPathConstants.BOOLEAN, new OaiPmhNamespaceContext());
	return xpathHelper.evaluate(node, xpath);
      } catch (XPathExpressionException e) {
	String message = "getBoolean: XPath Expression Exception: ";
	logger.error(message + xpath, e);
	throw new TransformerException(message + xpath, e);
      }
    }

    public String getString(String xpath) throws TransformerException {
      return getString(xpath, getDocument());
    }

    public String getString(String xpath, Node node) throws TransformerException {
      try {
	XPathHelper<String> xpathHelper = new XPathHelper<String>(XPathConstants.STRING, new OaiPmhNamespaceContext());
	return xpathHelper.evaluate(node, xpath);
      } catch (XPathExpressionException e) {
	String message = "getString: XPath Expression Exception: ";
	logger.error(message + xpath, e);
	throw new TransformerException(message + xpath, e);
      }
    }

    public Number getNumber(String xpath) throws TransformerException {
      return getNumber(xpath, getDocument());
    }

    public Number getNumber(String xpath, Node node) throws TransformerException {
      try {
	XPathHelper<Number> xpathHelper = new XPathHelper<Number>(XPathConstants.NUMBER, new OaiPmhNamespaceContext());
	return xpathHelper.evaluate(node, xpath);
      } catch (XPathExpressionException e) {
	String message = "getNumber: XPath Expression Exception: ";
	logger.error(message + xpath, e);
	throw new TransformerException(message + xpath, e);
      }
    }

    public Node getNode(String xpath) throws TransformerException {
      return getNode(xpath, getDocument());
    }

    public Node getNode(String xpath, Node node) throws TransformerException {
      try {
	XPathHelper<Node> xpathHelper = new XPathHelper<Node>(XPathConstants.NODE, new OaiPmhNamespaceContext());
	return xpathHelper.evaluate(node, xpath);
      } catch (XPathExpressionException e) {
	String message = "getNumber: XPath Expression Exception: ";
	logger.error(message + xpath, e);
	throw new TransformerException(message + xpath, e);
      }
    }

    public String toString() {
        Source input = new DOMSource(getDocument());
        StringWriter sw = new StringWriter();
        Result output = new StreamResult(sw);
        try {
            createTransformer().transform(input, output);
            return sw.toString();
        } catch (TransformerException e) {
            return e.getMessage();
        }
    }

    public boolean isUseTagSoup() {
      return useTagSoup;
    }

    public void setUseTagSoup(boolean useTagSoup) {
      this.useTagSoup = useTagSoup;
    }

    public int getHttpRetries() {
      return httpRetries;
    }

    public void setHttpRetries(int httpRetries) {
      this.httpRetries = httpRetries;
    }

    public int getHttpRetryWait() {
      return httpRetryWait;
    }

    public void setHttpRetryWait(int httpRetryWait) {
      this.httpRetryWait = httpRetryWait;
    }

    public int getHttpTimeout() {
      return httpTimeout;
    }

    public void setHttpTimeout(int httpTimeout) {
      this.httpTimeout = httpTimeout;
    }
}
