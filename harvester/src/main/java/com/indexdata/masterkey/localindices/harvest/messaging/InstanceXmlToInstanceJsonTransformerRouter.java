package com.indexdata.masterkey.localindices.harvest.messaging;

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

/**
 *
 * This class will take a Record with Instance XML (a XML document corresponding to
 * the JSON schema for FOLIO instances) and transform it to a JSON output.
 * <br/>
 * Currently supported is <br/>
 * <br>
 * Simple elements:  &lt;title&gt;My title&lt;/title&gt; <br/>
 *  transformed to "title": "My title"<br/>
 * <br/>
 * Arrays of simple elements: &lt;subjects&gt;&lt;arr&gt;&lt;i&gt;subject 1&lt;/i&gt;&lt;i&gt;subject 2&lt;/i&gt;&lt;/arr&gt;&lt;/subjects&gt;<br/>
 *  transformed to "subjects": ["subject 1", "subject 2"]<br/>
 * <br/>
 * Arrays of objects: &lt;publication&gt;&lt;arr&gt;&lt;i&gt;&lt;publisher&gt;a publisher&lt;/publisher&gt;&lt;place&gt;a place&lt;/place&gt;&lt;/i&gt;&lt;i&gt;...&lt;/i&gt;&lt;/arr&gt;&lt;/publication&gt;<br/>
 *  transformed to "publication": [{ "publisher": "a publisher", "place": "a place"}, {...}]<br/>
 *
 */
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
        if (recordIn.getSubRecords().isEmpty()) {
          logger.debug("Empty record came in from queue, skipping further processing of this document");
        } else {
          logger.debug(this.getClass().getSimpleName() + " has Record with " + recordIn.getSubRecords().size() + " sub record(s). Iterating:");
          RecordJSON recordOut = new RecordJSONImpl();
          JSONObject jsonRecords = new JSONObject();
          if (recordIn.isCollection()) {
            logger.debug(this.getClass().getSimpleName() + " found collection, adding Instance JSONs to collection:[]");
            Collection<Record> subrecords = recordIn.getSubRecords();
            jsonRecords.put("collection", new JSONArray());
            int i=0;
            for (Record rec : subrecords) {
              try {
                logger.debug("Sub record " + ++i);
                JSONObject json = makeInstanceJson(rec);
                ((JSONArray)(jsonRecords.get("collection"))).add(json);
              } catch(Exception e) {
                logger.error("Error adding record: " + e.getLocalizedMessage(), e);
              }
            }
            recordOut.setJsonObject(jsonRecords);
            recordOut.setOriginalContent(recordIn.getOriginalContent());
          } else {
            logger.debug("Record is not a collection, creating one Instance JSON");
            JSONObject jsonRecord = makeInstanceJson(recordIn);
            recordOut.setJsonObject(jsonRecord);
            recordOut.setOriginalContent(recordIn.getOriginalContent());
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

  /**
   * Recursively transforms incoming XML to JSON.
   * Would not handle arrays of arrays but otherwise handles arbitrary nesting,
   * ie arrays of objects themselves containing arrays and objects.
   * @param record
   * @return a JSON representation of the XML record
   */
  private static JSONObject makeInstanceJson(Record record) {

    JSONObject instanceJson = new JSONObject();
    NodeList nodeList = ((RecordDOM) record).toNode().getChildNodes();

    for (Node node : iterable(nodeList)) {
      if (isSimpleElement(node)) {
        instanceJson.put(node.getLocalName(), node.getTextContent());
      } else if (isObject(node)) {
        instanceJson.put(node.getLocalName(), makeJsonObject(node));
      } else if (isArray(node)) {
        JSONArray jsonArray = new JSONArray();
        NodeList items = node.getFirstChild().getChildNodes();
        for (Node item : iterable(items)) {
          if (isSimpleElement(item)) {
            jsonArray.add(item.getTextContent());
          } else if (isObject(item)) {
            jsonArray.add(makeJsonObject(item));
          }
        }
        instanceJson.put(node.getLocalName(), jsonArray);
      }
    }
    return instanceJson;
  }

  private static JSONObject makeJsonObject (Node node) {
    JSONObject jsonObject = new JSONObject();
    NodeList objectProperties = node.getChildNodes();
    for (Node objectProperty : iterable(objectProperties)) {
      if (isSimpleElement(objectProperty)) {
        jsonObject.put(objectProperty.getLocalName(), objectProperty.getTextContent());
      } else if (isObject(objectProperty)) {
        jsonObject.put(objectProperty.getLocalName(), makeJsonObject(objectProperty));
      } else if (isArray(objectProperty)) {
        JSONArray jsonArray = new JSONArray();
        NodeList items = objectProperty.getFirstChild().getChildNodes();
        for (Node item : iterable(items)) {
          if (isSimpleElement(item)) {
            jsonArray.add(item.getTextContent());
          } else if (isObject(item)) {
            jsonArray.add(makeJsonObject(item));
          }
        }
        jsonObject.put(objectProperty.getLocalName(), jsonArray);
      }

    }
    return jsonObject;
  }

  /**
   *
   * @param node
   * @return true if element is simple, non-empty scalar
   */
  private static boolean isSimpleElement(Node node) {
    return (node.hasChildNodes()
            && node.getFirstChild().getNodeType() == Node.TEXT_NODE
            && !node.getFirstChild().getTextContent().isEmpty());
  }

  /**
   *
   * @param node
   * @return true if element is a structure with sub-elements
   */
  private static boolean isObject(Node node) {
    return (node.hasChildNodes()
            && node.getFirstChild().getNodeType() == Node.ELEMENT_NODE
            && ! isArray(node));
  }

  /**
   *
   * @param node
   * @return true if element is an array (contains repeatable 'item' elements)
   */
  private static boolean isArray(Node node) {
    return (node.hasChildNodes()
            && node.getFirstChild().getLocalName().equals("arr"));
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
