package com.indexdata.masterkey.localindices.harvest.messaging;

public class ConsumerProxy<T> implements MessageProducer<T> {

  AsyncMessageConsumer<T> proxy;
  public ConsumerProxy(AsyncMessageConsumer<T> consumer) {
    proxy = consumer;
  }
  
  @Override
  public void put(T object) throws InterruptedException {
    proxy.onMessage(object);
  }

  
}
