package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.ByteArrayInputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.ErrorMessage;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordText;
import com.indexdata.xml.factory.XmlFactory;

@SuppressWarnings({ "rawtypes" })
public class XmlTransformerRouter implements MessageRouter {
  SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();

  private MessageConsumer input;
  private MessageProducer output;
  private MessageProducer error;
  private Transformer transformer;

  private boolean running = true;
  RecordHarvestJob job;
  StorageJobLogger logger;
  
  XmlTransformationStep step; 
  

  public XmlTransformerRouter(TransformationStep step, RecordHarvestJob job) {
    this.job = job;
    this.logger = job.getLogger();
    if (step instanceof XmlTransformationStep) {
      this.step = (XmlTransformationStep) step;
    }
    else throw new RuntimeException("Configuration Error: Not a XmlTransformationStep");
  }
  
  @Override
  public void run() {
    if (input == null) {
      throw new RuntimeException("Input queue not configured");
    }
    while (running)
      try {
	consume(input.take());
      } catch (InterruptedException e) {
	logger.warn("Interrupted. Running: " + running);
	e.printStackTrace();
      }
  }

  @SuppressWarnings({ "unchecked" })
  public void consume(Object take) {
    if (take instanceof StopMessage) {
      running = false;
      // Forward the StopMessage
      produce(take);
      return;
    }
    if (take instanceof Record) {
      Record record = (Record) take;
      Source xmlSource;
      xmlSource = extractSource(record);
      DOMResult result = new DOMResult();
      try {
	transformer.transform(xmlSource, result);
      } catch (Exception e) {
	try {
	  error.put(new ErrorMessage(xmlSource, e));
	  return;
	} catch (InterruptedException ie) {
	  logger.error("Failed to put ErrorMessage to Error Queue." + " Stack trace of Exception",
	      e);
	  return;
	}
      }
      if (record instanceof RecordDOM) {
	((RecordDOM) record).setNode(result.getNode());
      } else
	record = new RecordDOMImpl(record.getId(), record.getDatabase(), result.getNode());
      produce(record);
    }
  }

  private Source extractSource(Record take) {
    Source xmlSource = null;
    if (take instanceof RecordDOM) {
      RecordDOM record = (RecordDOM) take;
      Node node = record.toNode();
      xmlSource = new DOMSource(node);
    } else if (take instanceof RecordText) {
      RecordText record = (RecordText) take;
      xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(record.toText().getBytes())));
    }
    return xmlSource;
  }

  @SuppressWarnings("unchecked")
  private void produce(Object result) {
    try {
      output.put(result);
    } catch (InterruptedException e) {
      logger.error(
	  "Failed to put Result to Output queue: Interrupted. Attempt to save on Error Queue", e);
      try {
	error.put(new ErrorMessage(result, e));
      } catch (InterruptedException ie) {
	logger.error("Failed to put Result on Error Queue. Loosing message: " + result.toString());
      }
      e.printStackTrace();
    }
  }

  @Override
  public void setInput(MessageConsumer input) {
    this.input = input;
  }

  @Override
  public void setOutput(MessageProducer output) {
    this.output = output;
  }

  @Override
  public void setError(MessageProducer error) {
    this.error = error;
  }

  @Override
  public void shutdown() {
    running = false;
  }

  public void setXmlTransformer(Transformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public void onMessage(Object object) {
    consume(object);
  }

  @Override
  public Object take() throws InterruptedException {
    throw new RuntimeException("Not implemented");
  }

}
