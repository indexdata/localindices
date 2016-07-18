package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import ORG.oclc.oai.harvester2.verb.HarvesterVerb;

/**
 * Parses the XML stored in HarvesterVerb responses and builds ResponseDataObjects from it.
 *  
 * @author Niels Erik
 *
 */
public class OaiPmhResponseParser extends DefaultHandler {

  private XMLReader xmlReader = null;
  private ResponseDataObject currentElement = null;
  private Stack<ResponseDataObject> dataElements = new Stack<ResponseDataObject>();
  private ResponseDataObject result = null;
  private String xml = null;
  private static Logger logger = Logger.getLogger(OaiPmhResponseParser.class);

  public static List<String> docTypes = Arrays.asList("OAI-PMH");
  
  public OaiPmhResponseParser() {
    try {
      initSax();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }
  
  public static OaiPmhResponseParser getParser() {
    return new OaiPmhResponseParser();
  }
  
  private void initSax() throws ParserConfigurationException, SAXException {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();
    xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(this);         
  }
  
  /**
   * Parses a OAI-PMH XML response and produces a 
   * ResponseDataObject object.
   * 
   * @param response XML response string from Pazpar2
   * @return Response data object
   */
  public ResponseDataObject getDataObject (HarvesterVerb response) {
    this.xml = response.toString();
    try {      
      xmlReader.parse(new InputSource(new ByteArrayInputStream(response.toString().getBytes())));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace(); 
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();      
    }
    return result;
  }

  /** 
   * Receive notification at the start of element 
   * 
   */
  @Override
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("OAI-PMH")) {
      currentElement = new OaiPmhResponse ();
    } else if (localName.equals("ListSets")) {
      currentElement = new Sets();
    } else if (localName.equals("ListMetadataFormats")) {
      currentElement = new MetadataFormats();
    } else if (localName.equals("set")) {
      currentElement = new Set();
    } else if (localName.equals("metadataFormat")) {
      currentElement = new MetadataFormat();
    } else if (localName.matches("^[Ii]dentify$")) {
      currentElement = new Identify();
    // Catch all
    } else {
      currentElement = new ResponseDataObject();
    }
    currentElement.setType(localName);
    for (int i=0; i< atts.getLength(); i++) {
       currentElement.setAttribute(atts.getLocalName(i), atts.getValue(i));
    }    
    if (!docTypes.contains(localName)) {
      if (dataElements.size() == 0) {
        logger.info("Encountered unknown top level element [" + localName + "]. Creating generic data object.");
        currentElement.setType(localName);
      } else {
        dataElements.peek().addElement(localName, currentElement);
      }
    }
    if (this.xml != null) { // Store XML for doc level elements
      currentElement.setXml(xml);
      xml = null;
    }
    dataElements.push(currentElement);    
  }
 
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String data = new String(ch, start, length);        
    dataElements.peek().appendContent(data);    
  }
  
  @Override
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    if (dataElements.size()==1) {
      result = dataElements.pop();
    } else {
      dataElements.pop();
    }
  }
}
