package com.indexdata.masterkey.localindices.harvest.job;

import javax.xml.transform.Source;

import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.xml.filter.MessageConsumer;

public class RecordStorageConsumer implements MessageConsumer {

  RecordStorage recordStorage; 
  
  public RecordStorageConsumer(RecordStorage storage) {
    recordStorage = storage;
  }
  
  @Override
  public void accept(Node xmlNode) {
    Record record = new RecordDOMImpl(null, null, xmlNode);
    recordStorage.add(record);
  }

  @Override
  public void accept(Source xmlNode) {
    // TODO Auto-generated method stub

  }

}
