package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
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
import com.indexdata.utils.XmlUtils;
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
  Thread workerThread = null;
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
      logger.log(Level.TRACE, "Step " + this.step.getName() + " produced: " + sourceAsString(xmlSource));
      DOMResult result = new DOMResult();
      try {
        logger.log(Level.TRACE, "Calling transformer of type " + transformer.getClass().getName());
        transformer.transform(xmlSource, result);
      } catch (Exception e) {
        putError(xmlSource, e);
        return;
      }
      if (record instanceof RecordDOM) {
        ((RecordDOM) record).setNode(result.getNode());
      } else {
        record = new RecordDOMImpl(record.getId(), record.getDatabase(), result.getNode(), record.getOriginalContent());
      }
      produce(record);
    }
  }

  @SuppressWarnings("unchecked")
  private void putError(Source xmlSource, Exception e) {
    try {
      if (error != null)
        error.put(new ErrorMessage(xmlSource, e));
      else {
        logger.debug("No Error Message Router defined. Configure a Null Message Router to avoid this logging");
        logger.error("Error converting XML " + xmlSource, e);
      }
    } catch (Exception ie) {
      logger.error("Failed to put ErrorMessage to Error Queue." + " Stack trace of Exception",
          e);
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
      if (job.isKillSent())
      	  return ;
      logger.error(
	  "Failed to put Result to Output queue: Interrupted. Attempt to save on Error Queue", e);
      try {
	if (error != null)
	  error.put(new ErrorMessage(result, e));
	else
	  logger.error("Not Error Queue. Loosing message: " + result.toString());
      } catch (Exception ie) {
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
    if (workerThread != null)
      workerThread.interrupt();
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

  @Override
  public void setThread(Thread thread) {
    workerThread = thread;
  }

  private String sourceAsString (Source xmlSource) {
    try {
      StringWriter writer = new StringWriter();
      transformer.transform(xmlSource, new StreamResult(writer));
      return writer.getBuffer().toString();
    } catch (Exception e) {
      logger.log(Level.TRACE, e.getMessage());
      return "[could not write xml source as string]";
    }
  }
}
