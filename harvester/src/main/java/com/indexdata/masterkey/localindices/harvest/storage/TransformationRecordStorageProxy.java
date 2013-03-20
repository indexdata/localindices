package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.HarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.messaging.BlockingMessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.ConsumerProxy;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageProducer;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageRouter;
import com.indexdata.masterkey.localindices.harvest.messaging.RouterFactory;

public class TransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  private List<TransformationStep> steps;
  
  private HarvestJob job;
  private MessageRouter<Object>[] messageRouters;
  private MessageProducer<Object> source;
  private MessageQueue<Object> result = new BlockingMessageQueue<Object>();
  private MessageQueue<Object> error = new BlockingMessageQueue<Object>();
  
  public TransformationRecordStorageProxy(RecordStorage storage, List<TransformationStep> steps, StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.steps = steps;
    this.logger = logger;
    setupRouters();
  }

  protected Record transformNode(Record record) throws InterruptedException {
    source.put(record);
    if (!result.isEmpty()) {
	Object obj = result.take();
	if (obj instanceof Record)
	  return (Record) obj;
	else {
	  logger.error("Unknown type to add: " + obj.getClass() + " " + obj.toString());
	}
    }
    return null;
  }
    

  @Override 
  public void add(Record record) {
    RecordDOMImpl recordDOM = new RecordDOMImpl(record);
    while (true)
      try {
	Record transformed = transformNode(recordDOM);
	if (transformed != null)
	  storage.add(transformed);
	else {
	  logger.warn("Record filtered out" + record);
	}
	break;
      } catch (InterruptedException e) {
	e.printStackTrace();
	try {
	  error.put(e);
	} catch (InterruptedException e1) {
	  logger.error("Record not added to error" + record);
	  e1.printStackTrace();
	}
      }
  }
  @Override
  public void commit() throws IOException {
    super.commit();
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

  @SuppressWarnings("unchecked")
  protected void setupRouters() {
    if (steps != null) {
      messageRouters = new MessageRouter[steps.size()];
      RouterFactory factory = RouterFactory.newInstance(logger);
      int index = 0;
      MessageRouter<Object> previous = null;
      for (TransformationStep step : steps) {
	MessageRouter<Object> router = factory.create(step);
	if (source == null)
	  source = new ConsumerProxy<Object>(router);
	if (previous != null)
	  previous.setOutput(new ConsumerProxy<Object>(router));
	messageRouters[index++] = router;
	previous = router;
      }
      previous.setOutput(result);
    }
    
    if (source == null)
      source = result;
  }
}
