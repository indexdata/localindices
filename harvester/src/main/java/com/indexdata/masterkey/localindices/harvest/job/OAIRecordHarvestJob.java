/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import ORG.oclc.oai.harvester2.verb.OaiPmhException;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.notification.Notification;
import com.indexdata.masterkey.localindices.notification.NotificationException;
import com.indexdata.masterkey.localindices.notification.Sender;
import com.indexdata.masterkey.localindices.notification.SenderFactory;
import com.indexdata.masterkey.localindices.notification.SimpleMailSender;
import com.indexdata.masterkey.localindices.notification.SimpleNotification;
import com.indexdata.masterkey.localindices.util.TextUtils;
import java.text.DateFormat;
import java.util.TimeZone;

/**
 * This class is an implementation of the OAI-PMH protocol and may be used by
 * the scheduler through the HarvestJob interface. This class updates some of
 * the Harvestable's properties excluding the STATUS, the status has to be
 * handled on the higher level.
 * 
 * @author jakub
 * @author Dennis
 * 
 */
public class OAIRecordHarvestJob extends AbstractRecordHarvestJob {

  private OaiPmhResource resource;
  private Proxy proxy;
  private final static String SHORT_DATE_FORMAT = "yyyy-MM-dd";
  private final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
  private final DateFormat df;
  private boolean initialRun = true;
  private boolean moveUntilIntoFrom = false;

  @Override
  public String getMessage() {
    return resource.getMessage();
  }

  private class OAIError {
    private String code;
    private String message;

    OAIError(String code, String message) {
      this.code = code;
      this.message = message;
    }

    public String getCode() {
      return code;
    }
  }

  public OAIRecordHarvestJob(OaiPmhResource resource, Proxy proxy) {
    if (resource.getUrl() == null) {
      throw new IllegalArgumentException("baseURL parameter cannot be null");
    }
    if (resource.getMetadataPrefix() == null || resource.getMetadataPrefix().length() == 0) {
      resource.setMetadataPrefix("oai_dc");
    }
    if (resource.getOaiSetName() != null && resource.getOaiSetName().length() == 0) {
      resource.setOaiSetName(null);
    }
    if (resource.getDateFormat() != null) {
      df = new SimpleDateFormat(resource.getDateFormat());
    } else {
      df = new SimpleDateFormat(SHORT_DATE_FORMAT);
    }
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    this.resource = resource;
    this.proxy = proxy;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    if (getStatus().equals(HarvestStatus.NEW) || getStatus().equals(HarvestStatus.ERROR))
      this.initialRun = true;
    // this.resource.setMessage(null);
  }

  @Override
  public void run() {
    // TODO: Remove
    if (logger == null) 
      logger = new FileStorageJobLogger(this.getClass(), resource);
    setStatus(HarvestStatus.RUNNING);
    resource.setMessage(null);

    // in OAI date ranges are inclusive
    Date now = new Date();
    if (resource.getUntilDate() == null) {
        resource.setUntilDate(now);
    }
    // we don't need to compare from/until dates, let the server fail it
    try {
      getStorage().begin();
      harvest(resource.getUrl(), formatDate(resource.getFromDate()),
	  formatDate(resource.getUntilDate()), resource.getMetadataPrefix(),
	  resource.getOaiSetName(), resource.getResumptionToken(), getStorage());
    } catch (OaiPmhException e) {
      if (!isKillSent()) {
	setStatus(HarvestStatus.ERROR, e.getMessage());
	logOaiPmhException(e, e.getMessage());
      }
    } catch (IOException e) {
      if (!isKillSent()) {
	setStatus(HarvestStatus.ERROR, e.getMessage());
	resource.setMessage(e.getMessage());
	logger.log(Level.ERROR, e.getMessage());
	e.printStackTrace();
      }
    } catch (Exception e) {
      if (isKillSent()) {
	logger.log(Level.INFO, "Shutting down.");
      }
      else {
	logger.log(Level.WARN, "Recieved uncaught exception while running: " + e.getClass() + " " + e.getMessage());
	e.printStackTrace();
      }
    }
    // if there was no error we move the time marker
    if (getStatus() == HarvestStatus.OK || getStatus() == HarvestStatus.WARN) {
      resource.setFromDate(now);
      resource.setUntilDate(null);
      resource.setResumptionToken(null);
      if (getStatus() == HarvestStatus.OK) /* Do not reset WARN state */ 
	setStatus(HarvestStatus.FINISHED);
      logger.log(Level.INFO, "OAI harvest finished with status " + getStatus() + ". Next from: " + resource.getFromDate());
      try {
	getStorage().commit();
      } catch (IOException e) {
	logger.log(Level.ERROR, "Storage commit failed due to I/O Exception", e);
      }
    } else {
      logger.warn("Terminated with non-OK status: Job status " + getStatus());
      // We do not want to override a ERROR mesage, but should reset a killed/running status. 
      // Perhaps even leave killed, just be sure that we will start the job in this state. 
      if (getStatus().equals(HarvestStatus.KILLED) || getStatus().equals(HarvestStatus.RUNNING))
	setStatus(HarvestStatus.FINISHED);
      try {
	if (resource.getResumptionToken() != null) {
	  logger.log(Level.INFO, "OAI harvest killed/faced error "
	      + "- Commiting up to Resumption Token " + resource.getResumptionToken());
	  getStorage().commit();
	}
	else {
	  logger.log(Level.INFO, "OAI harvest killed/faced error - Rollback until " + resource.getFromDate());
	  getStorage().rollback();
	}
      } catch (IOException ioe) {
	logger.log(Level.ERROR, "Storage commit/rollback failed.");
      }
    }
    if (getStatus() != HarvestStatus.OK) {
	Sender sender = SenderFactory.getSender();
	String status = getStatus().toString();
	Notification msg = new SimpleNotification(status, resource.getName(), resource.getMessage());
	try {
	  sender.send(msg);
	} catch (NotificationException e1) {
	  logger.error("Failed to send notification " + resource.getMessage()) ;
	}
    }
    logger.close();
  }

  private void harvest(String baseURL, String from, String until, String metadataPrefix,
      String setSpec, String resumptionToken, RecordStorage storage) throws TransformerException, IOException 
      {

    ListRecords listRecords = null;
    if (resumptionToken == null || "".equals(resumptionToken)) {
      logger.log(Level.INFO, "OAI harvest started. Harvesting from: "  
	  + formatDate(resource.getFromDate()) + " until: " + formatDate(resource.getUntilDate()) + ", date format used as shown.");
      listRecords = listRecords(baseURL, from, until, setSpec, metadataPrefix);
    } else {
      logger.log(Level.INFO, "OAI harvest restarted using Resumption Token " + resource.getResumptionToken() + ".");
      listRecords = listRecords(baseURL, resumptionToken);
    }
    boolean dataStart = false;
    while (listRecords != null && !isKillSent()) {
      NodeList errorNodes = null;
      try {
	errorNodes = listRecords.getErrors();
      } catch (TransformerException te) {
	throw new IOException("Cannot read OAI-PMH protocol errors.", te);
      }
      OAIError[] errors = getErrors(errorNodes);
      if (errors != null) {
	// The error message has been logged, but print out the full record
	logger.log(Level.DEBUG, "OAI error response: \n" + listRecords.toString());
	// if this is noRecordsMatch and initial run, something is wrong
	logger.log(Level.DEBUG, "Parsed errors: #Errors: "
	    + errors.length + " First ErrorCode: " + errors[0].getCode() + " initialRun: " + initialRun);
	if (errors.length == 1 && errors[0].getCode().equalsIgnoreCase("noRecordsMatch")) {
	  logger.log(Level.INFO, "noRecordsMatch experienced for non-initial harvest - ignoring");
	  setStatus(HarvestStatus.WARN, "No Records matched");
	  return;
	} 
	else
	  throw new OaiPmhException("OAI error " + errors[0].code + ": " + errors[0].message, listRecords.getDocument());
      } else {
	if (!dataStart) {
	  getStorage().begin();
	  storage.databaseStart(resource.getId().toString(), null);
	  if (storage.getOverwriteMode())
	    storage.purge(false);
	  dataStart = true;
	}
	NodeList list;
	try {
	  list = listRecords.getRecords();
	  int count = list.getLength();
	  for (int index = 0; index < count; index++) {
	    Node node = list.item(index);
	    Record record = createRecord(node);
	    if (record.isDeleted())
	      getStorage().delete(record.getId());
	    else
	      getStorage().add(record);
	  }
	  resumptionToken = listRecords.getResumptionToken();
	} catch (TransformerException e) {
	  e.printStackTrace();
	  throw e;
	}
      }
      if (resumptionToken == null || resumptionToken.length() == 0) {
	logger.log(Level.INFO, "Records stored. No resumptionToken received, harvest done.");
	setStatus(HarvestStatus.OK);
	break;
      } else {
	logger.log(Level.INFO, "Records stored, next resumptionToken is " + resumptionToken);
	resource.setResumptionToken(resumptionToken);
	//getStorage().commit();
	markForUpdate();
	listRecords = listRecords(baseURL, resumptionToken);
      }
    }
    if (dataStart)
      getStorage().databaseEnd();
  }

  private void logOaiPmhException(OaiPmhException e, String string) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      Result xml = new StreamResult();
      transformer.transform(new DOMSource(e.getDocument()), xml);
      logger.error("Failed to get " + string + " from OAI-PMH XML response: " + xml.toString());
    } catch (TransformerConfigurationException e1) {
      logger.error("Failed to Transformer to serialize XML Document on error: " + e.getMessage());
      e1.printStackTrace();
    } catch (TransformerFactoryConfigurationError e1) {
      logger.error("Failed to Transformer to serialize XML Document on error: " + e.getMessage());
      e1.printStackTrace();
    } catch (TransformerException e1) {
      logger.error("Failed to serialize XML Document on error: " + e.getMessage());
      e1.printStackTrace();
    }
  }

  private RecordDOMImpl createRecord(Node node) throws TransformerException 
  {
    String id = HarvesterVerb.getSingleString(node, "./oai20:header/oai20:identifier/text()");
    String isDeleted = HarvesterVerb.getSingleString(node, "attribute::status"); 
    RecordDOMImpl record = new RecordDOMImpl(id, resource.getId().toString(), node);
    if ("deleted".equalsIgnoreCase(isDeleted)) 
      record.setDeleted(true);
    return record;
  }

  private ListRecords listRecords(String baseURL, String from, String until, String setSpec,
      String metadataPrefix) throws IOException {
    try {
      return new ListRecords(baseURL, from, until, setSpec, metadataPrefix, proxy, resource.getEncoding());
    } catch (ResponseParsingException hve) {
      String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
      //dumping  the response may cause ioexception
      try {
        logger.log(Level.DEBUG, msg + " Erroneous respponse:\n"
            + TextUtils.readStream(hve.getResponseStream()));
      } catch (IOException io) {
        logger.log(Level.ERROR, "IO exception when trying to dump bad ListRecords response - "+io.getMessage());
      }
      throw new IOException(msg, hve);
    } catch (IOException io) {
      throw io;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private ListRecords listRecords(String baseURL, String resumptionToken) throws IOException {
    try {
      return new ListRecords(baseURL, resumptionToken, proxy, resource.getEncoding());
    } catch (ResponseParsingException hve) {
      String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
      //dumping  the response may cause ioexception
      try {
        logger.log(Level.DEBUG, msg + " Erroneous respponse:\n"
            + TextUtils.readStream(hve.getResponseStream()));
      } catch (IOException io) {
        logger.log(Level.ERROR, "IO exception when trying to dump bad ListRecords response - "+io.getMessage());
      }
      throw new IOException(msg, hve);
    } catch (IOException io) {
      throw io;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private OAIError[] getErrors(NodeList errorNodes) {
    if (errorNodes != null && errorNodes.getLength() > 0) {
      int length = errorNodes.getLength();
      OAIError[] errors = new OAIError[length];
      for (int i = 0; i < length; ++i) {
	Node item = errorNodes.item(i);
	String code = item.getAttributes().getNamedItem("code").getNodeValue();
	String message = item.getTextContent();
	errors[i] = new OAIError(code, message);
	logger.log(Level.WARN, "OAI harvest error - " + code + ": " + message);
      }
      return errors;
    }
    return null;
  }

  private String formatDate(Date date) {
    if (date == null)
      return null;
    return df.format(date);
  }

  @Override
  public void setStorage(HarvestStorage storage) {
    if (!(storage instanceof RecordStorage))
      throw new RuntimeException("Requires a RecordStorage");
    setStorage((RecordStorage) storage);
  }

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("No implemented!");
  }

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }

}