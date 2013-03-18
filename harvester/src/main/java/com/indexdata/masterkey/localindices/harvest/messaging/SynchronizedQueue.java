package com.indexdata.masterkey.localindices.harvest.messaging;


public class SynchronizedQueue<T> implements MessageQueue<T> {
  AsyncMessageConsumer<T> next; 
  
  @Override
  public void put(T object) throws InterruptedException {
    next.onMessage(object);
  }
  
  @Override
  public T take() throws InterruptedException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean isEmpty() {
    throw new RuntimeException("Not implemented");  
    //return false;
  }
}
