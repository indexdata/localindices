package com.indexdata.masterkey.localindices.harvest.messaging;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class BlockingMessageQueue<T> implements MessageQueue<T> {

  BlockingQueue<T> messageQueue = new LinkedBlockingQueue<T>();
  
  @Override
  public T take() throws InterruptedException {
    return messageQueue.take();
  }

  @Override
  public void put(T e) throws InterruptedException {
    messageQueue.put(e);
  }

  @Override
  public boolean isEmpty() {
    return messageQueue.isEmpty();
  }

}
