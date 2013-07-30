
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;

/**
 * This class represents an ListRecords response on either the server or
 * on the client
 *
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class ListRecords extends HarvesterVerb {
    /**
     * Mock object constructor (for unit testing purposes)
     */
    public ListRecords() {
        super();
    }
    
    public ListRecords(Logger jobLogger) {
      super(jobLogger);
  }
    /**
     * Client-side ListRecords verb constructor
     *
     * @param baseURL the baseURL of the server to be queried
     * @exception MalformedURLException the baseURL is bad
     * @exception SAXException the xml response is bad
     * @exception IOException an I/O error occurred
     */
    public ListRecords(String baseURL, String from, String until,
            String set, String metadataPrefix, Proxy proxy, String encodingOverride, Logger logger)
    throws IOException, ParserConfigurationException, ResponseParsingException,
    TransformerException {
        super(getRequestURL(baseURL, from, until, set, metadataPrefix), proxy, encodingOverride, logger);
    }
    
    /**
     * Client-side ListRecords verb constructor (resumptionToken version)
     * @param baseURL
     * @param resumptionToken
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    public ListRecords(String baseURL, String resumptionToken, Proxy proxy, String encodingOverride, Logger logger)
    throws IOException, ParserConfigurationException, ResponseParsingException,
    TransformerException {
        super(getRequestURL(baseURL, resumptionToken), proxy, encodingOverride, logger);
    }
    
    /**
     * Get the oai:resumptionToken from the response
     * 
     * @return the oai:resumptionToken value
     * @throws TransformerException
     * @throws OaiPmhException
     */
    public String getResumptionToken()
    throws TransformerException {
        String schemaLocation = getSchemaLocation();
        if (schemaLocation.indexOf(SCHEMA_LOCATION_V2_0) != -1) {
            return getSingleString("/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken");
        } else if (schemaLocation.indexOf(SCHEMA_LOCATION_V1_1_LIST_RECORDS) != -1) {
            return getSingleString("/oai11_ListRecords:ListRecords/oai11_ListRecords:resumptionToken");
        } else {
          logger.error("Unknown schema location: " + schemaLocation + ". Looking for resumptiontoken without namespace. ");
          String resumptionToken = getSingleString("//resumptionToken");
          if (resumptionToken != null && !"".equals(resumptionToken))
            return resumptionToken;
          logger.error("Unknown schema location: " + schemaLocation + ". Looking for resumptiontoken without namespace (V1.1) ");
          resumptionToken = getSingleString("/ListRecords/resumptionToken");
          if (resumptionToken != null && !"".equals(resumptionToken))
            return resumptionToken;          
          throw new OaiPmhException("Error parsing Resumption Token from Document with Schema Location: " + schemaLocation, getDocument());
        }
    }

    /**
     * Get the oai:records from the response
     * 
     * @return the oai:records NodeList
     * @throws TransformerException
     * @throws OaiPmhException
     */
    public NodeList getRecords() throws TransformerException {
        String schemaLocation = getSchemaLocation();
        if (schemaLocation.indexOf(SCHEMA_LOCATION_V2_0) != -1) {
            return getNodeList("/oai20:OAI-PMH/oai20:ListRecords/oai20:record");
        } else if (schemaLocation.indexOf(SCHEMA_LOCATION_V1_1_LIST_RECORDS) != -1) {
            return getNodeList("/oai11_ListRecords:ListRecords");
        } else {
          logger.error("Unknown schema location: " + schemaLocation + ". Attempting alternatives without namespaces.");
          // TODO Can these be combined into one? 
          NodeList nodeList = getNodeList("/OAI-PMH/ListRecords/record");
          if (nodeList != null && nodeList.getLength() > 0)
            return nodeList;
          nodeList = getNodeList("/ListRecords/record");
          if (nodeList != null && nodeList.getLength() > 0)
            return nodeList;
          throw new OaiPmhException("Error parsing Records from Document using Schema Location: " + schemaLocation, getDocument());
        }
    }

    /**
     * Construct the query portion of the http request
     *
     * @return a String containing the query portion of the http request
     */
    private static String getRequestURL(String baseURL, String from,
            String until, String set,
            String metadataPrefix) {
        StringBuffer requestURL =  new StringBuffer(baseURL);
        requestURL.append("?verb=ListRecords");
        if (from != null) requestURL.append("&from=").append(from);
        if (until != null) requestURL.append("&until=").append(until);
        if (set != null) requestURL.append("&set=").append(set);
        requestURL.append("&metadataPrefix=").append(metadataPrefix);
        return requestURL.toString();
    }
    
    /**
     * Construct the query portion of the http request (resumptionToken version)
     * @param baseURL
     * @param resumptionToken
     * @return
     * @throws UnsupportedEncodingException 
     */
    private static String getRequestURL(String baseURL,
            String resumptionToken) throws UnsupportedEncodingException {
        StringBuffer requestURL =  new StringBuffer(baseURL);
        requestURL.append("?verb=ListRecords");
        requestURL.append("&resumptionToken=").append(URLEncoder.encode(resumptionToken, "UTF-8"));
        return requestURL.toString();
    }
}
