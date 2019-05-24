/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.xml.transform.Transformer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.ErrorMessage;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOM;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSONImpl;

public class XmlToJsonTransformerRouter implements MessageRouter {

  private MessageConsumer input;
  private MessageProducer output;
  private MessageProducer error;
  private Transformer transformer;

  private boolean running = true;

  RecordHarvestJob job;
  StorageJobLogger logger;
  Thread workerThread = null;
  TransformationStep step;


  public XmlToJsonTransformerRouter(TransformationStep step, RecordHarvestJob job) {
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
    Map<String, Collection<Serializable>> values = record.getValues();

    Serializable title = values.containsKey("title") ? values.get("title").iterator().next() : "";
    instanceJson.put("title", title);

    instanceJson.put("instanceTypeId", "6312d172-f0cf-40f6-b27d-9fa8feaf332f");
    instanceJson.put("source", "HARVEST");

    if (values.containsKey("author")) {
      JSONArray contributors = new JSONArray();
      for (Serializable author : values.get("author")) {
        JSONObject contributor = new JSONObject();
        contributor.put("name", author);
        contributor.put("contributorNameTypeId", "2b94c631-fca9-4892-a730-03ee529ffe2a");
        contributor.put("primary", true);
        contributor.put("contributorTypeId", "6e09d47d-95e2-4d8a-831b-f777b8ef6d81");
        contributors.add(contributor);
      }
      instanceJson.put("contributors", contributors);
    }

    if (values.containsKey("subject")) {
      JSONArray subjects = new JSONArray();
      for (Serializable subject : values.get("subject")) {
        subjects.add(subject);
      }
      instanceJson.put("subjects", subjects);
    }

    if (values.containsKey("description")) {
      JSONArray notes = new JSONArray();
      for (Serializable description : values.get("description")) {
        notes.add(description);
      }
      instanceJson.put("notes", notes);
    }

    if (values.containsKey("publication-name")) {
      JSONArray publication = new JSONArray();
      for (Serializable publicationName : values.get("publication-name")) {
        JSONObject publisher = new JSONObject();
        publisher.put("publisher", publicationName);
        publication.add(publisher);
      }
      instanceJson.put("publication", publication);
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

}
