package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class SplitTransformationChainRecordStorageProxy extends RecordStorageProxy {
  private PipedOutputStream output;
  private PipedInputStream input;
  private Thread thread = null;
  private TransformerException transformException = null;
  private StorageJobLogger logger; 
  private boolean closed = false;
  
  public SplitTransformationChainRecordStorageProxy(RecordStorage storage, final XMLReader xmlFilter, RecordHarvestJob job) 
  	throws IOException, TransformerConfigurationException {
    this.logger = job.getLogger();
    this.
    setTarget(storage);
    input = new PipedInputStream();
    output = new PipedOutputStream(input) {

      	public void close() throws IOException {
      	  setClosed(true);
      	  super.close();
      	}
    };
    thread = new Thread(new Runnable() {
      public void run() {
	processDataFromInputStream(input);
      };

      private void processDataFromInputStream(PipedInputStream input) {
	try {
	  InputSource source = new InputSource(input);
	  xmlFilter.parse(source);
	} catch (IOException ioe) {
	  transformException = new TransformerException("IO Error while parsing/transforming: "
	      + ioe.getMessage(), ioe);
	  if (logger != null) 
	    logger.error("IOException in XML split", ioe);
	  ioe.printStackTrace();
	} catch (SAXException e) {
	  if (logger != null) 
	    logger.error("SAXException in XML split", e);
	  e.printStackTrace();
	  transformException = new TransformerException("SAX Exception: " + e.getMessage(), e);
	}
      };
    });
    thread.start();
  }

  @Override
  public void commit() throws IOException {
    try {
      // Close the output so the PipedInputStream will get the EOF.
      output.flush();
      output.close();
      output = null;
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Ensure that the PipedInputStream has read it all and the transformation
    // has finished
    try {
      thread.join();
      thread = null;
    } catch (InterruptedException e) {
      if (logger != null) 
	logger.error("Interrupted before joined", e);
      e.printStackTrace();
    }
    if (transformException != null) {
      throw new IOException("Transformation failed: " + transformException.getMessage(), transformException);
    }
    super.commit();
  }

  public OutputStream getOutputStream() {
    return output;
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return storage.getStatus();
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return storage.getContentHandler();
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  @Override
  public void shutdown() {
      try {
	if (output != null) {
	  logger.error("Closing Storage before commit/rollback");
	  output.flush();
	  output.close();
	}
	if (thread != null)
	  thread.join();
      } catch (Exception e) {
	logger.warn("Exception while closing: " + e.getMessage());
	e.printStackTrace();
      }
    super.shutdown();
  }

}
