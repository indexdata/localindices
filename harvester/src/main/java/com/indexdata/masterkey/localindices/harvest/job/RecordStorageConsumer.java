package com.indexdata.masterkey.localindices.harvest.job;

import javax.xml.transform.Source;

import org.apache.log4j.Level;
import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.utils.XmlUtils;
import com.indexdata.xml.filter.MessageConsumer;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;

public class RecordStorageConsumer implements MessageConsumer {

  public final static int ORIGINAL_BUFF_SIZE = 10000;
  RecordStorage recordStorage; 
  StorageJobLogger logger;
  long added = 0;
  boolean storeOriginal;
  ByteArrayOutputStream originalBuff;
  
  public RecordStorageConsumer(RecordStorage storage, StorageJobLogger logger, boolean storeOriginal) {
    recordStorage = storage;
    this.logger = logger;
    this.storeOriginal = storeOriginal;
    originalBuff = new ByteArrayOutputStream(ORIGINAL_BUFF_SIZE);
  }
  
  @Override
  public void accept(Node xmlNode) {
    long creationStart = System.currentTimeMillis();
    logger.log(Level.TRACE, "Document in pipeline for storage: " + nodeAsString(xmlNode));
    byte[] original = null;
    originalBuff.reset();
    try {
      XmlUtils.serialize(xmlNode, originalBuff);
    } catch (TransformerException ex) {
      logger.error("Failed to serialize original contents for storage, record num "+added);
    }
    original = originalBuff.toByteArray();
    Record record = new RecordDOMImpl(null, null, xmlNode, original);
    record.setCreationTiming(System.currentTimeMillis()-creationStart);
    try {
      if (record.isDeleted()) {
        recordStorage.delete(record);
      } else {
        recordStorage.add(record);
        if (++added % 1000 == 0)
          logger.info("Fetched " + added + " records.");
      }
    } catch (RuntimeException ioe) {
      	String msg = "Failed to add record." + record;
      	logger.info(msg);
    	throw ioe;
    }
  }

  @Override
  public void accept(Source xmlNode) {
    // TODO Auto-generated method stub

  }

  private String nodeAsString (Node xmlNode) {
    try {
      StringWriter writer = new StringWriter();
      XmlUtils.serialize(xmlNode, writer);
      return writer.toString();
    } catch (Exception e) {e.printStackTrace();
    return "";}

  }

}
