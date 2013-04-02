package com.indexdata.masterkey.localindices.harvest.job;

import java.util.Queue;

@SuppressWarnings("rawtypes")
public interface MessageRouter extends Runnable {

  void setInput(Queue input);
  void setOutput(Queue output);
  void setError(Queue error);
  void shutdown();
  

}
