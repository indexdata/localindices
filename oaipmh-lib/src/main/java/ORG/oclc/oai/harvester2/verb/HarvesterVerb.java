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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
//import org.apache.xpath.XPathAPI;
import com.sun.org.apache.xpath.internal.XPathAPI;
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
@SuppressWarnings("restriction")
public abstract class HarvesterVerb {
    private static Logger logger = Logger.getLogger("org.oclc.oai.harvester2");

    private final static int HTTP_MAX_RETRIES = 10;
    private final static int HTTP_RETRY_TIMEOUT = 600; //secs

    /* Primary OAI namespaces */
    public static final String SCHEMA_LOCATION_V2_0 = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
    public static final String SCHEMA_LOCATION_V1_1_GET_RECORD = "http://www.openarchives.org/OAI/1.1/OAI_GetRecord http://www.openarchives.org/OAI/1.1/OAI_GetRecord.xsd";
    public static final String SCHEMA_LOCATION_V1_1_IDENTIFY = "http://www.openarchives.org/OAI/1.1/OAI_Identify http://www.openarchives.org/OAI/1.1/OAI_Identify.xsd";
    public static final String SCHEMA_LOCATION_V1_1_LIST_IDENTIFIERS = "http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers.xsd";
    public static final String SCHEMA_LOCATION_V1_1_LIST_METADATA_FORMATS = "http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats.xsd";
    public static final String SCHEMA_LOCATION_V1_1_LIST_RECORDS = "http://www.openarchives.org/OAI/1.1/OAI_ListRecords http://www.openarchives.org/OAI/1.1/OAI_ListRecords.xsd";
    public static final String SCHEMA_LOCATION_V1_1_LIST_SETS = "http://www.openarchives.org/OAI/1.1/OAI_ListSets http://www.openarchives.org/OAI/1.1/OAI_ListSets.xsd";
    private Document doc = null;
    private String schemaLocation = null;
    private String requestURL = null;
    private static HashMap<Thread, DocumentBuilder> builderMap = new HashMap<Thread, DocumentBuilder>();
    private static Element namespaceElement = null;
    private static DocumentBuilderFactory factory = null;
    private boolean useTagSoup = false;
    private static HashMap<Thread, TransformerFactory> transformerFactoryMap = new HashMap<Thread, TransformerFactory>();
    private static HashMap<Thread, XPathFactory> xpathFactoryMap = new HashMap<Thread, XPathFactory>();
    private static boolean debug = false;
    
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
    public NodeList getErrors() throws TransformerException {
        if (SCHEMA_LOCATION_V2_0.equals(getSchemaLocation())) {
            //return getNodeList("/oai20:OAI-PMH/oai20:error");
          return getNodeList("/OAI-PMH/error");
        } else {
            return null;
        }
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
     * Performs the OAI request
     * 
     * @param requestURL
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    public HarvesterVerb(String requestURL, Proxy proxy) throws IOException,
    ParserConfigurationException, TransformerException, ResponseParsingException {
        harvest(requestURL, proxy);
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
    public void harvest(String requestURL, Proxy proxy) throws IOException,
    ParserConfigurationException, TransformerException, ResponseParsingException {
        this.requestURL = requestURL;
        logger.log(Level.INFO, "requestURL=" + requestURL);
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
            con.setRequestProperty("Accept-Encoding",
            "compress, gzip, identify");
            try {
                responseCode = con.getResponseCode();
                logger.log(Level.INFO,"responseCode=" + responseCode);
            } catch (FileNotFoundException e) {
                // response is majorly broken, retry nevertheless
                logger.log(Level.INFO, requestURL, e);
                responseCode = -1;
            }
            //for some responses the server will tell us when to retry
            //for others we'll use the defaults
            if (responseCode == -1
             || responseCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT
             || responseCode == HttpURLConnection.HTTP_ENTITY_TOO_LARGE
             || responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR
             || responseCode == HttpURLConnection.HTTP_BAD_GATEWAY
             || responseCode == HttpURLConnection.HTTP_UNAVAILABLE
             || responseCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
                long retrySeconds = con.getHeaderFieldInt("Retry-After", -1);
                if (retrySeconds == -1) {
                    //this is because in HTTP date may be already parsed as seconds
                    long now = (new Date()).getTime();
                    long retryDate = con.getHeaderFieldDate("Retry-After", now);
                    retrySeconds = retryDate - now;
                }
                if (retrySeconds == 0) { //header not specified
                    retrySeconds = HTTP_RETRY_TIMEOUT;
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
                retry = ++totalRetries < HTTP_MAX_RETRIES;
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
        logger.log(Level.INFO, "contentEncoding=" + contentEncoding);
        if ("compress".equals(contentEncoding)) {
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
        

        InputStream bin = new BufferedInputStream(new FailsafeXMLCharacterInputStream(in));
        bin.mark(contentLength);
        InputSource data = new InputSource(bin);        
	try {
	  if (isUseTagSoup()) 
	    doc = createTagSoupDocument(data);
	  else 
	    doc = createDocument(data);
	} catch (SAXException saxe) {
          bin.reset();
          saxe.printStackTrace();
          throw new ResponseParsingException("Cannot parse response: " + saxe.getMessage(),
                  saxe, bin, requestURL);
	}
	if (logger.isDebugEnabled() && debug) {
	  Transformer transformer = createTransformer();
	  transformer.transform(new DOMSource(doc), new StreamResult(System.out));
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
    
  public String getSingleString(Node node, String xpath) throws TransformerException {
      return XPathAPI.eval(node, xpath, namespaceElement).str();
  }
    
    /**
     * Get a NodeList containing the nodes in the response DOM for the specified
     * xpath
     * @param xpath
     * @return the NodeList for the xpath into the response DOM
     * @throws TransformerException
     */
  
  static HashMap<String, XPathExpression> xPathExprMap = new HashMap<String, XPathExpression>();
  
    public NodeList getNodeList(String xpath) throws TransformerException {
      //return XPathAPI.selectNodeList(getDocument(), xpath, namespaceElement);
      try {
	XPathExpression expr = xPathExprMap.get(xpath); 
	if (expr == null) {
	      XPath xPath = createXPath();
	      expr = xPath.compile(xpath);
	      xPathExprMap.put(xpath, expr);
	}
	return (NodeList) expr.evaluate(getDocument(), XPathConstants.NODESET);
      } catch (XPathExpressionException e) {
	logger.error("XPath Exception: ", e);
      }
      return null;
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
}
