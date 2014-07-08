package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import com.indexdata.masterkey.localindices.client.StopException;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.messaging.BlockingMessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.ConsumerProxy;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageProducer;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageRouter;
import com.indexdata.masterkey.localindices.harvest.messaging.RouterFactory;

public class TransformationRecordStorageProxy extends AbstractTransformationRecordStorageProxy  {
  private StorageJobLogger logger;
  private List<TransformationStep> steps;
  
  private RecordHarvestJob job;
  private MessageRouter<Object>[] messageRouters;
  private MessageProducer<Object> source;
  private MessageQueue<Object> result = new BlockingMessageQueue<Object>();
  private MessageQueue<Object> errors = new BlockingMessageQueue<Object>();
  private int count = 0;
  private Integer limit = null;
  
  public TransformationRecordStorageProxy(RecordStorage storage, List<TransformationStep> steps, RecordHarvestJob job) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.steps = steps;
    this.job = job;
    this.logger = job.getLogger();
    if (job.getHarvestable().getRecordLimit() != null) {
      limit = job.getHarvestable().getRecordLimit();
    }
    setupRouters();
  }

  protected Record transformNode(Record record) throws InterruptedException {
    source.put(record);
    if (!result.isEmpty()) {
	Object obj = result.take();
	count++;
	if (obj instanceof Record)
	  return (Record) obj;
	else {
	  logger.error("Unknown type to add: " + obj.getClass() + " " + obj.toString());
	}
    }
    return null;
  }
    
  private void testLimit() {
    if (limit != null && limit > 0 && count >= limit) {
      String msg = "Stop requested after " + limit + " records";
      logger.info(msg);
      throw new StopException(msg);
    }
  }

  @Override 
  public void add(Record record) {
    if (job.isKillSent())
      throw new RuntimeException("Job killed");
    RecordDOMImpl recordDOM = new RecordDOMImpl(record);
    while (true)
      try {
	Record transformed = transformNode(recordDOM);
	if (transformed != null)
	  getTarget().add(transformed);
	else {
	  logger.warn("Record filtered out" + record);
	}
	testLimit();
	break;
      } catch (InterruptedException e) {
	e.printStackTrace();
	try {
	  errors.put(e);
	} catch (InterruptedException e1) {
	  logger.error("Record not added to error" + record);
	  e1.printStackTrace();
	}
      }
  }

  @Override
  public void delete(String id) {
    RecordDOMImpl recordDOM = new RecordDOMImpl(id, null, null);
    while (true) {
      if (job.isKillSent())
	      throw new RuntimeException("Job killed");
      try {
	Record transformed = transformNode(recordDOM);
	if (transformed != null)
	  getTarget().delete(transformed.getId());
	else {
	  logger.warn("Record filtered out. " + recordDOM);
	}
	break;
      } catch (InterruptedException e) {
	e.printStackTrace();
	try {
	  errors.put(e);
	} catch (InterruptedException e1) {
	  logger.error("Record not added to error. " + recordDOM);
	  e1.printStackTrace();
	}
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
      RouterFactory factory = RouterFactory.newInstance(job);
      int index = 0;
      MessageRouter<Object> previous = null;
      for (TransformationStep step : steps) {
	MessageRouter<Object> router = factory.create(step);
	router.setError(errors);
	if (source == null)
	  source = new ConsumerProxy<Object>(router);
	if (previous != null)
	  previous.setOutput(new ConsumerProxy<Object>(router));
	messageRouters[index++] = router;
	previous = router;
      }
      if (previous != null) {
        previous.setOutput(result);
      } else {
        logger.warn("Empty transformation, no normalization performed");
      }
    }
    if (source == null)
      source = result;
  }

  public MessageQueue<Object> getErrors() {
    return errors;
  }

  public void setErrors(MessageQueue<Object> errors) {
    this.errors = errors;
  }
}
