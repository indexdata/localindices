package com.indexdata.masterkey.localindices.harvest.messaging;


public interface AsyncMessageConsumer<T> extends MessageConsumer<T> {
  
  void onMessage(T object);
  
}
