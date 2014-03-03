package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.harvest.messaging.MessageQueue;

public interface ErrorQueue {
  
  MessageQueue<Object> getErrors();
  void setErrors(MessageQueue<Object> errors);
  

}
