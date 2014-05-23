package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.utils.XmlUtils;

public class XmlSplitter  {
  private StorageJobLogger logger; 
  private ContentHandler handler;

  public XmlSplitter(RecordStorage storage, StorageJobLogger logger, ContentHandler handler) 
  	throws IOException, TransformerConfigurationException {
    this.logger = logger;
    this.handler = handler;
  }

  public void processDataFromInputStream(InputStream input) throws TransformerException, ParserConfigurationException, SAXException 
  {    
    try {
      InputSource source = new InputSource(input);
      XmlUtils.read(source, handler);
    } catch (IOException ioe) {
      if (logger != null) 
	logger.error("IOException in XML split", ioe);
      throw new TransformerException("IO Error while parsing/transforming: " + ioe.getMessage(), ioe);
    } catch (SAXException e) {
      if (logger != null) 
        logger.error("SAXException in XML split", e);
      throw new TransformerException("SAX Exception: " + e.getMessage(), e);
    }
  };
}
