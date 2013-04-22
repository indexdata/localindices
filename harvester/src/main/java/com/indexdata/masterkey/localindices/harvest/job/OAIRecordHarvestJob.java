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

import javax.xml.transform.TransformerException;

import org.apache.log4j.Level;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.ListRecords;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.util.TextUtils;

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
  private final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
  private String currentDateFormat;
  private boolean initialRun = true;

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
      currentDateFormat = resource.getDateFormat();
    } else {
      currentDateFormat = DEFAULT_DATE_FORMAT;
    }
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

    // figure out harvesting period, even though we may end up using resumptionTokens from the DB
    if (resource.getUntilDate() == null)
      resource.setUntilDate(yesterday());
    Date nextFrom = plusOneDay(resource.getUntilDate());
    try {
      getStorage().begin();
      harvest(resource.getUrl(), formatDate(resource.getFromDate()),
	  formatDate(resource.getUntilDate()), resource.getMetadataPrefix(),
	  resource.getOaiSetName(), resource.getResumptionToken(), getStorage());
    } catch (IOException e) {
      setStatus(HarvestStatus.ERROR);
      resource.setMessage(e.getMessage());
      logger.log(Level.DEBUG, e.getMessage(), e);
    }
    // if there was no error we move the time marker
    if (getStatus() == HarvestStatus.OK) {
      // TODO persist until and from, trash resumption token
      resource.setFromDate(nextFrom);
      resource.setUntilDate(null);
      resource.setResumptionToken(null);
      setStatus(HarvestStatus.FINISHED);
      logger.log(Level.INFO, "OAI harvest finished OK. Next from: " + resource.getFromDate());
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
    logger.close();
  }

  private void harvest(String baseURL, String from, String until, String metadataPrefix,
      String setSpec, String resumptionToken, RecordStorage storage) throws IOException 
      {

    ListRecords listRecords = null;
    if (resumptionToken == null || "".equals(resumptionToken)) {
      logger.log(Level.INFO, "OAI harvest started. Harvesting from: "  
	  + resource.getFromDate() + " until: " + resource.getUntilDate());
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
	  setStatus(HarvestStatus.KILLED);
	  return;
	} else
	  throw new IOException("OAI error " + errors[0].code + ": " + errors[0].message);
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
	  throw new IOException("Transformation Exception: " + e.getMessage(), e);
	} catch (NoSuchFieldException e) {
	  e.printStackTrace();
	  throw new IOException("NoSuchFieldException: " + e.getMessage(), e);
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
      logger.log(Level.ERROR,
	  msg + " Erroneous respponse:\n" + TextUtils.readStream(hve.getResponseStream()));
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

  private Date yesterday() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -1); // back one
    return c.getTime();
  }

  private Date plusOneDay(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DAY_OF_MONTH, 1);
    return c.getTime();
  }

  private String formatDate(Date date) {
    if (date == null)
      return null;
    return new SimpleDateFormat(currentDateFormat).format(date);
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