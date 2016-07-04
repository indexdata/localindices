package com.indexdata.masterkey.localindices.harvest.job;

import javax.xml.transform.Source;

import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.utils.XmlUtils;
import com.indexdata.xml.filter.MessageConsumer;
import java.io.ByteArrayOutputStream;
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
    if (storeOriginal) {
      originalBuff = new ByteArrayOutputStream(ORIGINAL_BUFF_SIZE);
    }
  }
  
  @Override
  public void accept(Node xmlNode) {
    byte[] original = null;
    if (this.storeOriginal) {
      originalBuff.reset();
      try {
        XmlUtils.serialize(xmlNode, originalBuff);
      } catch (TransformerException ex) {
        logger.error("Failed to store original contents for record num "+added);
      }
      original = originalBuff.toByteArray();
    }
    Record record = new RecordDOMImpl(null, null, xmlNode, original);
    try {
      recordStorage.add(record);
      if (++added % 1000 == 0)
      	logger.info("Fetched " + added + " records.");
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

}
