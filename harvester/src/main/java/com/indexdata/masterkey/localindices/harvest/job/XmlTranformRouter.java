package com.indexdata.masterkey.localindices.harvest.job;

import java.io.StringBufferInputStream;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordText;

@SuppressWarnings({ "rawtypes", "deprecation" })
public class XmlTranformRouter implements MessageRouter {

  private BlockingQueue input; 
  private BlockingQueue output; 
  private BlockingQueue error; 
  private Transformer transformer; 
  
  private boolean running = true;
  Logger logger = Logger.getLogger(getClass());
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
  private void consume(Object take) {
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
      } catch (TransformerException e) {
	try {
	  error.put(new ErrorMessage(xmlSource, e));
	  return;
	} catch (InterruptedException ie) {
	  logger.error("Failed to put ErrorMessage to Error Queue." + 
	      		" Stack trace of Transformation Exception", e);
	return;
      }
    }
      if (record instanceof RecordDOM) {
	((RecordDOM) record).setNode(result.getNode());
      }
      else
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
      xmlSource = new SAXSource(new InputSource(new StringBufferInputStream(record.toText())));
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
  public void setInput(Queue input) {
    if (input instanceof BlockingQueue)
      this.input = (BlockingQueue) input;
    else 
      throw new RuntimeException("Requires a blocking input queue");
  }

  @Override
  public void setOutput(Queue output) {
    if (output instanceof BlockingQueue)
      this.output = (BlockingQueue) output;
    else 
      throw new RuntimeException("Requires a blocking output queue");
  }

  @Override
  public void setError(Queue error) {
    if (error instanceof BlockingQueue)
      this.error = (BlockingQueue) error;
    else 
      throw new RuntimeException("Requires a blocking error queue");
  }

  @Override
  public void shutdown() {
    running = false; 
  }

  public void setXmlTransformer(Transformer transformer) {
    this.transformer = transformer;
  }

}
