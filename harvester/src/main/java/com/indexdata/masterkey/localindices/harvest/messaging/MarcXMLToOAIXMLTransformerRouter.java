
package com.indexdata.masterkey.localindices.harvest.messaging;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.ErrorMessage;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class MarcXMLToOAIXMLTransformerRouter implements MessageRouter<Object> {
  private MessageConsumer<Object> input;
  private MessageProducer<Object> output;
  private MessageProducer<Object> error;
  
  private boolean running = true;
  
  public static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
  
  RecordHarvestJob job;
  StorageJobLogger logger;
  TransformationStep step;
  
  public MarcXMLToOAIXMLTransformerRouter(TransformationStep step, RecordHarvestJob job) {
    this.job = job;
    this.logger = job.getLogger();
    if (step instanceof TransformationStep) {
      this.step = (TransformationStep)step;
    } else {
      throw new RuntimeException("Configuration error, 'step' must be of type TransformationStep");
    }
  }

  @Override
  public void setInput(MessageConsumer<Object> input) {
    this.input = input;
  }

  @Override
  public void setOutput(MessageProducer<Object> output) {
    this.output = output;
  }

  @Override
  public void setError(MessageProducer<Object> error) {
    this.error = error;
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void setThread(Thread thred) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void run() {
    if(input == null) {
      throw new RuntimeException("Input queue is not configured");
    }
    while(running) {
      try {
        consume(input.take());
      } catch(InterruptedException ie) {
        logger.warn("Interrupted while Running: " + running + ": " + ie.getLocalizedMessage());
      }
    }
  }

  @Override
  public void onMessage(Object object) {
    consume(object);
  }

  @Override
  public Object take() throws InterruptedException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  private void consume(Object documentIn) {
    if(!(documentIn instanceof Record)) {
      return;
    }
    Record record = (Record)documentIn;
    try {
      Node recordNode = ((RecordDOM) record).toNode();
      if(recordNode.getNodeType() == Node.DOCUMENT_NODE) {
        recordNode = ((Document)recordNode).getDocumentElement();
      }
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbFactory.newDocumentBuilder();
      Document document = builder.newDocument();
      Element root = document.createElementNS(OAI_NAMESPACE, "record");
      document.appendChild(root);
      Element metadata = document.createElementNS(OAI_NAMESPACE, "metadata");
      root.appendChild(metadata);
      Node recordNodeCopy = document.importNode(recordNode, true);
      metadata.appendChild(recordNodeCopy);
      RecordDOMImpl recordOut = new RecordDOMImpl(record.getId(), null, document,
          record.getOriginalContent());
      produce(recordOut);
    } catch(Exception e) {
      logger.error("Error in consume: ", e);
    }
  }

  private void produce(Object documentOut) {
    try {
      output.put(documentOut);
    } catch(InterruptedException ie) {
      if(job.isKillSent()) {
        return;
      }
      logger.error("Failed to put Result to output queue: Interrupted", ie);
      try {
        if(error != null) {
          error.put(new ErrorMessage(documentOut, ie));
        } else {
          logger.error("No error queue, losing mesage: " + documentOut.toString());
        }
      } catch(InterruptedException ie2) {
        logger.error("Failed to put Result on Error queue, losing mesage: "
            + documentOut.toString());
      }
    } catch(Exception e) {
      logger.error("Error putting file on output queue: " + e.getLocalizedMessage());
    }
  }


}
