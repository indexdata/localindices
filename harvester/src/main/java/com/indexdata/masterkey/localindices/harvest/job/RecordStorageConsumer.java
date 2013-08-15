package com.indexdata.masterkey.localindices.harvest.job;

import javax.xml.transform.Source;

import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.xml.filter.MessageConsumer;

public class RecordStorageConsumer implements MessageConsumer {

  RecordStorage recordStorage; 
  StorageJobLogger logger;
  long added = 0; 
  public RecordStorageConsumer(RecordStorage storage, StorageJobLogger logger) {
    recordStorage = storage;
    this.logger = logger;
    
  }
  
  @Override
  public void accept(Node xmlNode) {
    Record record = new RecordDOMImpl(null, null, xmlNode);
    recordStorage.add(record);
    if (++added % 1000 == 0)
      	logger.info("Fetched " + added + " records.");
  }

  @Override
  public void accept(Source xmlNode) {
    // TODO Auto-generated method stub

  }

}
