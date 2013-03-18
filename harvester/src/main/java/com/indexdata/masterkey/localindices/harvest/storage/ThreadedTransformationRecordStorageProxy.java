package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.messaging.BlockingMessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageQueue;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageRouter;
import com.indexdata.masterkey.localindices.harvest.messaging.StopMessage;
import com.indexdata.masterkey.localindices.harvest.messaging.XmlTransformerRouter;

public class ThreadedTransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  //private Templates[] templates; 
  private MessageQueue<Object> source = new BlockingMessageQueue<Object>();
  private MessageQueue<Object> result;
  private MessageQueue<Object> error = new BlockingMessageQueue<Object>();
  private Thread lastThread = null;
  private List<TransformationStep> steps;
  private String databaseID; 
  
  
  public ThreadedTransformationRecordStorageProxy(RecordStorage storage, 
      	List<TransformationStep> steps, StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.steps = steps;
    this.logger = logger;
    setupRouters();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void setupRouters() throws TransformerConfigurationException
  {
    MessageQueue<Object> current = source; 
    MessageRouter router = null;
    int index = 0;
    if (steps != null) {
      for (TransformationStep step: steps) {
        if (step instanceof XmlTransformationStep) {
          XmlTransformerRouter xmlRouter = new XmlTransformerRouter(step);
          router = xmlRouter;
        }
        router.setError(error);
        router.setInput(current);
        current = new BlockingMessageQueue();
        router.setOutput(current);
        Thread thread = new Thread(router);
        thread.setName("XmlTransformerRouter " + index++); 
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
