package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.indexdata.masterkey.localindices.entity.CustomTransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.ErrorMessage;
import com.indexdata.masterkey.localindices.harvest.job.HarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordText;
import com.indexdata.xml.factory.XmlFactory;

@SuppressWarnings({ "rawtypes"})
public class XmlFileDumperRouter implements MessageRouter {
  SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();

  private MessageConsumer input;
  private MessageProducer output;
  private MessageProducer error;
  private Transformer transformer;
  private String charname = "UTF-8";
  private Thread workerThread = null;
  private boolean running = true;
  HarvestJob job;
  StorageJobLogger logger;
  CustomTransformationStep step; 
  OutputStream out = System.err; 
  int sampling = 1;
  int count = 0; 
  String filename;
  public XmlFileDumperRouter(TransformationStep step, RecordHarvestJob job) {
    logger = job.getLogger();
    if (step instanceof CustomTransformationStep) {
      this.step = (CustomTransformationStep) step;
      setup(this.step);
    }
    else throw new RuntimeException("Configuration Error: Not a XmlTransformationStep");
  }

  public XmlFileDumperRouter(CustomTransformationStep step, RecordHarvestJob job) {
    this.step = step;
    logger = job.getLogger();
    setup(step);
  }

  @SuppressWarnings("unchecked")
  private void setup(CustomTransformationStep step) {
    try {
      Map<String, Object> map = new HashMap<String, Object>();   
      if (step.getScript() != null) {
        logger.debug("Logger configuration: " + step.getScript());
        JSONParser parser = new JSONParser();
        Object config = parser.parse(step.getScript());
        if (config instanceof Map)
          map = (Map) config;
        else 
          logger.debug("Not an Map object?");
	transformer = stf.newTransformer();
      }
      else 
        logger.debug("Using Job Logger for XML logging. " + logger.getIdentify());
	
      if (map.containsKey("indent")) {
	logger.log(Level.DEBUG, "Configuring XML indent " + map.get("indent").toString() + "");
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", map.get("indent").toString());
      }
      if (map.containsKey("file")) {
	filename = map.get("file").toString();
	File file = new File(filename);
	boolean append = false;
	if (map.get("append") != null)
	  append = "true".equals(map.get("append"));
	out = new FileOutputStream(file, append);
      } 
      else {
	logger.debug("No file configured. Using standard out");
	out = System.out;
      }
      if (map.containsKey("sample")) {
	sampling = Integer.parseInt(map.get("sample").toString());
      }

    } catch (TransformerConfigurationException e) {
      logger.log(Level.ERROR, "Unable to create XML Transformer", e);
      e.printStackTrace();
    } catch (ParseException e) {
      logger.log(Level.ERROR, "Unable to parse JSON configuration: " + step.getScript(), e);
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
      try {
	out.close();
      } catch (IOException e) {
	logger.error("Failed to close file: " );
	e.printStackTrace();
      }
      logger.info("Got StopMessage. Parsing on, shutting down.");
      produce(take);
      return;
    }
    if (take instanceof Record) {
      Record record = (Record) take;
      Source xmlSource;
      xmlSource = extractSource(record);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      StreamResult result = new StreamResult(byteOutput);
      try {
	transformer.transform(xmlSource, result);
	if (out != null && (count++) % sampling == 0) {
	  out.write(byteOutput.toString().getBytes());
	  out.flush();
	}
	produce(record);
      } catch (Exception e) {
	logger.error("Failed to put Message ("+ result + ") on Output Queue.", e);
	try {
	  error.put(new ErrorMessage(xmlSource, e));
	  return;
	} catch (InterruptedException ie) {
	  logger.error("Failed to put ErrorMessage to Error Queue." + " Stack trace of Exception", e);
	  return;
	}
      }
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
      try {
	xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(record.toText().getBytes(charname))));
      } catch (UnsupportedEncodingException e) {
	e.printStackTrace();
      }
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
    return input.take();
    //return null;
  }

  @Override
  public void setThread(Thread thread) {
    workerThread = thread;
  }

}
