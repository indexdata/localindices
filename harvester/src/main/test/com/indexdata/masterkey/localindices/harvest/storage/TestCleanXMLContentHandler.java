package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import junit.framework.TestCase;

public class TestCleanXMLContentHandler extends TestCase {
  
  String data =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
      "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\"><responseDate>2012-01-11T20:14:24Z</responseDate>\n" + 
      " <request verb=\"ListRecords\" until=\"2012-01-10\" set=\"collection:texts\" metadataPrefix=\"oai_dc\">http://www.archive.org/services/oai2.php</request>\n" + 
      " <ListRecords>" +
      "  <record>" +
      "   <header>" +
      "     <identifier>oai:archive.org:1790_census</identifier>\n" + 
      "     <datestamp>2010-10-28T22:34:20Z</datestamp>\n" + 
      "     <setSpec>mediatype:collection</setSpec>\n" + 
      "     <setSpec>collection:us_census</setSpec>\n" + 
      "     <setSpec>collection:texts</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>" +
      "     <oai_dc:dc xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><dc:description><p> This microfilm was provided by the <a href=\"http://www.acpl.lib.in.us/genealogy/index.html\">Genealogy Center</a> at the <a href=\"http://www.acpl.lib.in.us/\">Allen County Public Library</a> in Fort Wayne, Indiana, and sponsored by the Internet Archive.</p> <p> 1st Population Census of the United States - 1790</p> <table align=\"center\" width=\"90%\"> <tr> <td valign=\"top\"> <ul> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Aconnecticut\">Connecticut</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Amaine\">Maine</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Amassachusetts\">Massachusetts</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Amaryland\">Maryland</a></li> </ul> </td> <td valign=\"top\"> <ul> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3A%28New%20Hampshire%29\">New Hampshire</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3A%28New%20York%29\">New York</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3A%28North%20Carolina%29\">North Carolina</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3APennsylvania\">Pennsylvania</a></li> </ul> </td> <td valign=\"top\"> <ul> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3A%28Rhode%20Island%29\">Rhode Island</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3A%28South%20Carolina%29\">South Carolina</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Avermont\">Vermont</a></li> <li><a href=\"http://www.archive.org/search.php?query=collection%3A1790_census%20AND%20subject%3Avirginia\">Virginia</a></li> </ul> </td> </tr> </table></dc:description>\n" + 
      "       <dc:format>Collection Header</dc:format>\n" + 
      "       <dc:format>JPEG</dc:format>\n" + 
      "       <dc:format>JPEG Thumb</dc:format>\n" + 
      "       <dc:format>Metadata</dc:format>" +
      "	      <dc:description><span></dc:description>" +
      "     </oai_dc:dc>" +
      "	  </metadata>" +
      "  </record>" +
      " </ListRecords>" +  
       "</OAI-PMH>";
  
  public void testcleanXMLReader() throws SAXException, TransformerFactoryConfigurationError, TransformerException, UnsupportedEncodingException {
    
    XMLReader reader = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
    boolean useNamespace = reader.getFeature("http://xml.org/sax/features/namespaces");
    boolean usePrefixes = reader.getFeature("http://xml.org/sax/features/namespace-prefixes");
    reader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
    Logger.getLogger(getClass()).debug("Namespace: " + useNamespace + ". Prefixes: " + usePrefixes);
    SAXTransformerFactory saxFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
      
    TransformerHandler handler = saxFactory.newTransformerHandler();
    //XMLFilter xmlFilter = saxFactory.newXMLFilter(); 
    //xmlFilter.setContentHandler(new CleanXMLContentHandler());
    handler.setResult(new StreamResult(new File("test")));
    InputStream inputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
    Source input = new SAXSource(reader, new InputSource(inputStream));
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    CleanXMLContentHandler contentHandler = new CleanXMLContentHandler();
    contentHandler.addProtectedElement("dc:description");
    contentHandler.addProtectedElement("description");
    SAXResult saxResult = new SAXResult(contentHandler);
    
    transformer.transform(input, saxResult);
    saxResult.toString();
  }

}
