package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

//import org.apache.commons.io.input.TeeInputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.xml.factory.XmlFactory;

public class TransformationChainRecordStorageProxy extends RecordStorageProxy {
  private PipedOutputStream output;
  private PipedInputStream input;
  private Thread thread = null;
  private SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  private Transformer transformer;
  private TransformerException transformException = null;
  private StorageJobLogger logger; 
  
  public TransformationChainRecordStorageProxy(final RecordStorage storage,
      final XMLReader xmlFilteredReader, final ContentHandler storageHandler, final StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    // this.xmlFilter = xmlFilter;
    this.logger = logger;
    setTarget(storage);
    input = new PipedInputStream();
    output = new PipedOutputStream(input);
    transformer = stf.newTransformer();
    // this.storageHandler = storageHandler;
    thread = new Thread(new Runnable() {
      public void run() {
	processDataFromInputStream(input);
      };

      private void processDataFromInputStream(PipedInputStream input) {
	InputSource source = new InputSource(input);
	SAXSource transformSource = new SAXSource(xmlFilteredReader, source);
	SAXResult result = new SAXResult(storageHandler);
	try {
	  transformer.transform(transformSource, result);
	} catch (TransformerException e) {
	  if (logger != null) 
	    logger.error("Transformation exception. Save for later ", e);
	  e.printStackTrace();
	  transformException = e;
	  try { 
	    output.close();
	  }
	  catch (IOException ioe) 
	  {
	    logger.error("IOException while closing output");
	  }
	}
      };
    });
    thread.start();
  }

  @Override
  public void commit() throws IOException {
    try {
      // Close the output so the PipedInputStream will get the EOF.
      output.close();
    } catch (IOException e) {
      if (logger != null) 
	logger.error("IOException on close.", e);
      e.printStackTrace();
    }
    // Ensure that the PipedInputStream has read it all and the transformation
    // has finished
    try {
      thread.join();
    } catch (InterruptedException e) {
      if (logger != null) 
	logger.error("Interrupted before joined", e);
      e.printStackTrace();
    }
    if (transformException != null) {
      if (logger != null) 
	logger.error("Throw saved Transformation exception as IOException", transformException);
      throw new IOException("Transformation failed", transformException);
    }
    super.commit();
  }

  public OutputStream getOutputStream() {
    return output;
  }

  public StorageJobLogger getLogger() {
    return logger;
  }

  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return getTarget().getStatus();
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return getTarget().getContentHandler();
  }

}
