package com.indexdata.masterkey.localindices.harvest.messaging;

public interface MessageProducer<T> {
  void put(T object) throws InterruptedException;
}
