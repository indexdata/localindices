package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.HarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.messaging.MessageRouter;
import com.indexdata.masterkey.localindices.harvest.messaging.XmlTransformerRouter;
import com.indexdata.xml.factory.XmlFactory;

public class TransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  private MessageRouter[] messageRouters; 
  //protected TransformerFactory stf = XmlFactory.newTransformerInstance();
  protected SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  private HarvestJob job;
  
  public TransformationRecordStorageProxy(RecordStorage storage, List<TransformationStep> steps, StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    // setupRouters(harvestable, steps);;
    this.logger = logger;
  }
  protected Source transformNode(Source xmlSource) throws TransformerException {
    //Transformer transformer;
    if (messageRouters == null) {
      /* DOMResult result = new DOMResult();
      Template nullTemplate = 
      transformer.transform(xmlSource, result);
       */
      return xmlSource;
      
    }
    
    // TODO non-parallel  
    for (MessageRouter router : messageRouters) {

      //router.run();
      break;
    }
    return xmlSource;
  }  

  @Override 
  public void add(Record record) {
    RecordDOMImpl recordDOM = new RecordDOMImpl(record);
    Node node = recordDOM.toNode();
    try {
      Source result = transformNode(new DOMSource(node));
      if (result instanceof DOMSource) {
	node = ((DOMSource) result).getNode();
	Record recordTransformed = new RecordDOMImpl(record.getId(), record.getDatabase(), node);
	super.add(recordTransformed);
      }
      else {
	// TODO implement to DOM transform? 
	logger.fatal("Non-DOM not implemented. Record lost.");
      }
    } catch (TransformerException te) {
      logger.warn("Unable to transform record" + record + ". Xml: " + node.toString());
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

  protected void setupRouters(Harvestable resource, List<TransformationStep> steps) {
    int index = 0;
    String stepInfo = "";
    String stepScript ="";
    MessageRouter<Object> previous = null;
    try {
      for (TransformationStep step : steps) {
        stepInfo =  step.getId() + " " + step.getName();
        if (step.getScript() != null) {
          stepScript = step.getScript();
          MessageRouter<Object> router = (MessageRouter<Object>) new XmlTransformerRouter(step);
          if (previous != null)
            previous.setOutput(null);
          previous = router;
        }
        else {
          logger.warn("Step " + stepInfo + " has not script!");
        }
        
      }
    } catch (Exception tce) {
      String error = "Failed to build xslt templates: " + stepInfo;

      logger.error("Failed to build XSLT template for Step: " + stepInfo + "Script: " + stepScript);      
      logger.error(error);
      job.setStatus(HarvestStatus.ERROR, error);
    }
  }

}
