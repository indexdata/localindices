package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import com.indexdata.masterkey.localindices.client.StopException;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.messaging.BlockingMessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageRouter;
import com.indexdata.masterkey.localindices.harvest.messaging.RouterFactory;
import com.indexdata.masterkey.localindices.harvest.messaging.StopMessage;

public class ThreadedTransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  //private Templates[] templates; 
  private MessageQueue<Object> source = new BlockingMessageQueue<Object>();
  private MessageQueue<Object> result;
  private MessageQueue<Object> error = new BlockingMessageQueue<Object>();
  private Thread lastThread = null;
  private RecordHarvestJob job; 
  private List<TransformationStep> steps;
  private String databaseID;
  private MessageRouter<Object>[] messageRouters;
  private int count = 0; 
  private Integer limit = null; 
  
  
  public ThreadedTransformationRecordStorageProxy(RecordStorage storage, 
      	List<TransformationStep> steps, RecordHarvestJob job) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.steps = steps;
    this.job = job;
    limit = job.getHarvestable().getRecordLimit();
    this.logger = job.getLogger();
    setupRouters();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void setupRouters() throws TransformerConfigurationException
  {
    MessageQueue<Object> current = source; 
    MessageRouter router = null;
    RouterFactory factory = RouterFactory.newInstance(job);
    if (steps != null) {
      int index = 0;
      messageRouters = new MessageRouter[steps.size()];
      for (TransformationStep step: steps) {
	router = factory.create(step);
	messageRouters[index++] = router;
        router.setError(error);
        router.setInput(current);
        current = new BlockingMessageQueue();
        router.setOutput(current);
        Thread thread = new Thread(router);
        thread.setName("Router(" + step.getName() + ")"); 
        router.setThread(thread);
        thread.start();
        lastThread = thread;
      }
    result = current;
    }
  }  

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    databaseID = database;
    super.databaseStart(database, properties);
  }

  @Override 
  public void add(Record record) {
    RecordDOMImpl recordDOM = new RecordDOMImpl(record);
    while (true) {
      try {
	source.put(recordDOM);
	break; 
      } catch (InterruptedException ie) {
	logger.warn("Unable to put record" + record + ". Xml: " + recordDOM.toNode().toString() + ". Retrying. ");
      }
    }
    store();
  }

  @Override 
  public void delete(String id) {
    RecordImpl record = new RecordImpl();
    record.setId(id);
    record.setDatabase(databaseID);
    
    while (true) {
      try {
	source.put(record);
	break; 
      } catch (InterruptedException ie) {
	logger.warn("Unable to put record" + record + ". Id: " + id + ". Retrying.");
      }
    }
    store();
  }

  private void store() {
    while (!result.isEmpty()) {
      Object object;
      try {
	object = result.take();
	if (object instanceof RecordDOMImpl) {
	  RecordDOMImpl record = (RecordDOMImpl) object;
	  if (record.isDeleted())
	    super.delete(record.getId());
	  else
	    super.add(record);
	  count++;
	  if (limit != null && limit > 0 && count >= limit) {
	    String msg = "Stop requested after " + limit + " records";
	    logger.info(msg);
	    throw new StopException(msg);
	  }
	}
	else {
	  logger.error("Unsupported message type: " + object.getClass());
	}
      } catch (InterruptedException e) {
	e.printStackTrace();
      }
    }
  }

  private void storeFinal() {
    Object object = null;
    while (! (object instanceof StopMessage)) {
      try {
	object = result.take();
      } catch (InterruptedException e) {
	e.printStackTrace();
	continue;
      }
      if (object instanceof RecordDOMImpl) {
	RecordDOMImpl record = (RecordDOMImpl) object;
	if (record.isDeleted())
	    super.delete(record.getId());
	else
	  super.add(record);
      }
      if (object instanceof StopMessage)
	logger.info("StopMessage");
    }
  }

  class DummyStopMessage  implements StopMessage {
    
  }
  @Override
  public void commit() throws IOException {
    while (true)
      try {
	source.put(new DummyStopMessage());
	if (lastThread != null)
	  lastThread.join(); 
	storeFinal();
	break;
      } catch (InterruptedException e) {
	e.printStackTrace();
      }
    super.commit();
 }
  
@Override
  public void rollback() throws IOException {
    try {
      for (MessageRouter<Object> router : messageRouters) {
	router.shutdown();
      }
      // Required to be sure no more messages is sent.
      if (lastThread != null)
	lastThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    super.rollback();
  }

  public StorageJobLogger getLogger() {
    return logger;
  }

  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return getTarget().getStatus();
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return getTarget().getContentHandler();
  }

}
