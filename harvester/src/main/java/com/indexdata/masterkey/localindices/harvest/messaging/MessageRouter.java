package com.indexdata.masterkey.localindices.harvest.messaging;


public interface MessageRouter<T> extends Runnable, AsyncMessageConsumer<T>  {
  void setInput(MessageConsumer<T> input);
  void setOutput(MessageProducer<T> output);
  void setError(MessageProducer<T> error);
  void shutdown();
  void setThread(Thread thred);
}
