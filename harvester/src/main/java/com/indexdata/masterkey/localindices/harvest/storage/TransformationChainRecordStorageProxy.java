package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
//import org.apache.commons.io.input.TeeInputStream;

public class TransformationChainRecordStorageProxy extends RecordStorageProxy {
  private PipedOutputStream output;
  private PipedInputStream input;
  private Thread thread = null;
  private StorageJobLogger logger;
  
  public TransformationChainRecordStorageProxy(RecordStorage storage, XMLReader xmlFilteredReader, 
      RecordHarvestJob job) throws IOException,
      TransformerConfigurationException {
    this.logger = job.getLogger();
    setTarget(storage);
    input = new PipedInputStream();
    output = new PipedOutputStream(input);
    thread = new Thread(new TransformPipedInputStream(input, output, storage.getContentHandler(), job));
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
/*
    if (transformException != null) {
      if (logger != null) 
	logger.error("Throw saved Transformation exception as IOException", transformException);
      throw new IOException("Transformation failed", transformException);
    }
*/
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
