package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import com.indexdata.masterkey.localindices.harvest.job.StopMessage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.XmlTranformRouter;

public class ThreadedTransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  private Templates[] templates; 
  private BlockingQueue<Object> source = new LinkedBlockingQueue<Object>();
  private BlockingQueue<Object> result;
  private BlockingQueue<Object> error = new LinkedBlockingQueue<Object>();
  private Thread lastThread = null;
  private String databaseID; 
  
  
  public ThreadedTransformationRecordStorageProxy(RecordStorage storage, Templates[] templates, StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.templates = templates;
    setupRouters();
    this.logger = logger;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void setupRouters() throws TransformerConfigurationException
  {
    BlockingQueue<Object> current = source; 
    int index = 0;
    if (templates != null)
      for (Templates template : templates) {
        XmlTranformRouter router = new XmlTranformRouter();
        Transformer transformer = template.newTransformer();
        router.setXmlTransformer(transformer);
        router.setError(error);
        router.setInput(current);
        current = new LinkedBlockingQueue();
        router.setOutput(current);
        Thread thread = new Thread(router);
        thread.setName("XmlTransformerRouter " + index++); 
        thread.start();
        lastThread = thread;
      }
    result = current;
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
      Object object = result.remove();
      if (object instanceof RecordDOMImpl) {
	RecordDOMImpl record = (RecordDOMImpl) object;
	if (record.isDeleted())
	    super.delete(record.getId());
	else
	  super.add(record);
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
