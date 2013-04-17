package com.indexdata.masterkey.localindices.harvest.messaging;

public interface MessageConsumer<T> {
  
  T take() throws InterruptedException;
  
}
