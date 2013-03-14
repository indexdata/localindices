package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.xml.factory.XmlFactory;

public class XmlSplitter  {
  private StorageJobLogger logger; 
  private ContentHandler handler;
  SAXParserFactory spf;

  public XmlSplitter(RecordStorage storage, StorageJobLogger logger, ContentHandler handler) 
  	throws IOException, TransformerConfigurationException {
    this.logger = logger;
    spf = XmlFactory.newSAXParserFactoryInstance();
    this.handler = handler;
  }

  public void processDataFromInputStream(InputStream input) throws TransformerException, ParserConfigurationException, SAXException 
  {
    SAXParser parser = spf.newSAXParser();
    XMLReader reader = parser.getXMLReader();
    reader.setContentHandler(handler);
    
    try {
      InputSource source = new InputSource(input);
      reader.parse(source);
    } catch (IOException ioe) {
      if (logger != null) 
	logger.error("IOException in XML split", ioe);
      ioe.printStackTrace();
      throw new TransformerException("IO Error while parsing/transforming: " + ioe.getMessage(), ioe);
    } catch (SAXException e) {
	if (logger != null) 
	  logger.error("SAXException in XML split", e);
	 e.printStackTrace();
	throw new TransformerException("SAX Exception: " + e.getMessage(), e);
    }
  };
}
