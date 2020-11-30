package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.utils.XmlUtils;

public class XmlSplitter  {
  private StorageJobLogger logger;
  private ContentHandler handler;
  private final boolean useLaxParsing;

  public XmlSplitter(StorageJobLogger logger,
    ContentHandler handler, boolean useLaxParsing)
  	throws IOException {
    this.logger = logger;
    this.handler = handler;
    this.useLaxParsing = useLaxParsing;

  }

  public void processDataFromInputStream(InputStream input) throws SAXException, IOException
  {
    try {
      logger.debug("XmlSplitter: Use lax parsing mode: "+useLaxParsing);
      InputSource source = new InputSource(input);
      logger.debug("XmlSplitter.processDataFromInputStream: read start");
      XmlUtils.read(source, handler, useLaxParsing);
      logger.debug("XmlSplitter.processDataFromInputStream() done");
    } catch (IOException ioe) {
      if (logger != null)
	logger.error("IOException in XML split", ioe);
      throw ioe;
    } catch (SAXException e) {
      if (logger != null)
        logger.error("SAXException in XML split", e);
      throw e;
    }
  };
}
