package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
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
  private BlockingQueue<Object> error;
  private Thread lastThread = null; 
  
  
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
        thread.setName("XmlTransfomrRouter " + index++); 
        thread.start();
        lastThread = thread;
      }
    result = current;
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

  private void store() {
    while (!result.isEmpty()) {
      Object object = result.remove();
      if (object instanceof RecordDOMImpl) {
	RecordDOMImpl record = (RecordDOMImpl) object;
	super.add(record);
      }
    }
  }

  class DummyStopMessage  implements StopMessage {
    
  }
  @Override
  public void commit() throws IOException {
    while (true)
      try {
	source.put(new DummyStopMessage());
	lastThread.join(); 
	store();
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
