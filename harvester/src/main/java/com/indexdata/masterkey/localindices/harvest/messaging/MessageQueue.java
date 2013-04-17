package com.indexdata.masterkey.localindices.harvest.messaging;


public interface MessageQueue<T> extends MessageConsumer<T>, MessageProducer<T> {
  boolean isEmpty();
  T take() throws InterruptedException;
  void put(T object)  throws InterruptedException;
}
