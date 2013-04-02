package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Node;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class TransformationRecordStorageProxy extends RecordStorageProxy {
  private StorageJobLogger logger;
  private Templates[] templates; 
  
  public TransformationRecordStorageProxy(RecordStorage storage, Templates[] templates, StorageJobLogger logger) throws IOException,
      TransformerConfigurationException {
    setTarget(storage);
    this.templates = templates;
    this.logger = logger;
  }
  protected Source transformNode(Source xmlSource) throws TransformerException {
    Transformer transformer;
    if (templates == null)
      return xmlSource;
    // TODO parallel with message queues? 
    for (Templates template : templates) {
      transformer = template.newTransformer();
      DOMResult result = new DOMResult();
      transformer.transform(xmlSource, result);
      
      if (result.getNode() == null) {
        logger.warn("transformNode: No Node found");
        xmlSource = new DOMSource();
      } else
        xmlSource = new DOMSource(result.getNode());
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
      }
      Record recordTransformed = new RecordDOMImpl(record.getId(), record.getDatabase(), node);
      super.add(recordTransformed);
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

}
