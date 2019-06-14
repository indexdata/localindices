package com.indexdata.masterkey.localindices.harvest.messaging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.ErrorMessage;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSONImpl;

public class InstanceXmlToInstanceJsonTransformerRouter implements MessageRouter {

  private MessageConsumer input;
  private MessageProducer output;
  private MessageProducer error;

  private boolean running = true;

  RecordHarvestJob job;
  StorageJobLogger logger;
  Thread workerThread = null;
  TransformationStep step;


  public InstanceXmlToInstanceJsonTransformerRouter (TransformationStep step, RecordHarvestJob job) {
    this.job = job;
    this.logger = job.getLogger();
    if (step instanceof TransformationStep) {
      this.step = (TransformationStep) step;
    }
    else throw new RuntimeException("Configuration Error: Not a TransformationStep");
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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setThread(Thread thred) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
      }
  }

  private void consume(Object documentIn) {
    if (documentIn instanceof Record) {
      Record recordIn = (Record) documentIn;
      try {
        if (((RecordDOM) recordIn).toNode().getChildNodes().getLength() == 0) {
          logger.info("Empty record came in from queue, skipping further processing of this document");
        } else {
          RecordJSON recordOut = new RecordJSONImpl();
          JSONObject jsonRecords = new JSONObject();
          if (recordIn.isCollection()) {
            Collection<Record> subrecords = recordIn.getSubRecords();
            jsonRecords.put("collection", new JSONArray());
            for (Record rec : subrecords) {
              try {
                JSONObject json = makeInstanceJson(rec);
                ((JSONArray)(jsonRecords.get("collection"))).add(json);
              } catch(Exception e) {
                logger.error("Error adding record: " + e.getLocalizedMessage(), e);
              }
            }
            recordOut.setJsonObject(jsonRecords);
          } else {
            JSONObject jsonRecord = makeInstanceJson(recordIn);
            recordOut.setJsonObject(jsonRecord);
          }
          produce(recordOut);
        }
      } catch (Exception e) {
        logger.error("Error in consume: ", e);
      }

    }
  }

  private void produce(Object documentOut) {
    try {
      output.put(documentOut);
    } catch (InterruptedException e) {
      if (job.isKillSent())
        return ;
      logger.error(
	  "Failed to put Result to Output queue: Interrupted. Attempt to save on Error Queue", e);
      try {
	if (error != null)
	  error.put(new ErrorMessage(documentOut, e));
	else
	  logger.error("No error queue. Loosing message: " + documentOut.toString());
      } catch (InterruptedException ie) {
	logger.error("Failed to put Result on Error Queue. Loosing message: " + documentOut.toString());
      }
      logger.error("");
    }
  }

  private JSONObject makeInstanceJson(Record record) {

    JSONObject instanceJson = new JSONObject();
    NodeList nodeList = ((RecordDOM) record).toNode().getChildNodes();

    for (Node node : iterable(nodeList)) {
      if (node.hasChildNodes() && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
        instanceJson.put(node.getLocalName(), node.getTextContent());
      } else {
        if (node.getLocalName().equals("contributors")) {
          JSONArray contributors = new JSONArray();
          NodeList items = node.getChildNodes();
          for (Node item : iterable(items)) {
            JSONObject jsonItem = new JSONObject();
            NodeList itemProperties = item.getChildNodes();
            for (Node itemProperty : iterable(itemProperties)) {
              jsonItem.put(itemProperty.getLocalName(), itemProperty.getTextContent());
            }
            contributors.add(jsonItem);
          }
          if (contributors.size()>0) instanceJson.put("contributors", contributors);
        }
        if (node.getLocalName().equals("publication")) {
          JSONArray publication = new JSONArray();
          NodeList items = node.getChildNodes();
          for (Node item : iterable(items)) {
            JSONObject jsonItem = new JSONObject();
            NodeList itemProperties = item.getChildNodes();
            for (Node itemProperty : iterable(itemProperties)) {
              jsonItem.put(itemProperty.getLocalName(), itemProperty.getTextContent());
            }
            publication.add(jsonItem);
          }
          if (publication.size()>0) instanceJson.put("publication", publication);
        }
        if (Arrays.asList("editions", "subjects").contains(node.getLocalName())) {
          JSONArray jsonItems = new JSONArray();
          NodeList items = node.getChildNodes();
          for (Node item : iterable(items)) {
              jsonItems.add(item.getTextContent());
          }
          if (jsonItems.size()>0) instanceJson.put(node.getLocalName(), jsonItems);
        }
      }
    }
    return instanceJson;
  }

  @Override
  public void onMessage(Object documentIn) {
    consume(documentIn);
  }

  @Override
  public Object take() throws InterruptedException {
     throw new RuntimeException("Not implemented");
  }

  private static Iterable<Node> iterable(final NodeList nodeList) {
    return () -> new Iterator<Node>() {

        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < nodeList.getLength();
        }

        @Override
        public Node next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return nodeList.item(index++);
        }
    };
}
}
