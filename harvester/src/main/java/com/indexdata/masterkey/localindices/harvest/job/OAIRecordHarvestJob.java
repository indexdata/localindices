/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import ORG.oclc.oai.harvester2.data.InputStreamWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
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
import com.indexdata.masterkey.localindices.harvest.cache.CachingInputStream;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import java.io.InputStream;
import static com.indexdata.utils.TextUtils.joinPath;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

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
  @SuppressWarnings("unused")
  private final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
  private final DateFormat df;
  private boolean initialRun = true;
  private int totalCount;

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
    try {
    if (logger == null) 
      logger = new FileStorageJobLogger(this.getClass(), resource);
    resource.setMessage(null);
    resource.setAmountHarvested(null);
    setStatus(HarvestStatus.RUNNING);
    String startResumptionToken = resource.getResumptionToken();
    // in OAI date ranges are inclusive
    Date now = new Date();
    if (resource.getUntilDate() == null) {
        resource.setUntilDate(now);
    }
    // we don't need to compare from/until dates, let the server fail it
    try {
      getStorage().setOverwriteMode(resource.getOverwrite());
      DiskCache dc = new DiskCache(resource.getId());
      if (resource.getOverwrite()) {
        if (!resource.isDiskRun()) dc.purge();
      }
      harvest(resource.getUrl(), formatDate(resource.getFromDate()),
	  formatDate(resource.getUntilDate()), resource.getMetadataPrefix(),
	  resource.getOaiSetName(), resource.getResumptionToken(), getStorage(), dc);
      if (HarvestStatus.RUNNING == getStatus()) {
	// This shouldn't be possible 
	logger.warn("Got RUNNING state at job end.");
	setStatus(HarvestStatus.OK);
      }
/*
    } catch (ResponseParsingException e) {
      if (!isKillSent()) {
	setStatus(HarvestStatus.ERROR, e.getMessage());
      
      }
*/      
    } catch (OaiPmhException e) {
      if (!isKillSent()) {
	setStatus(HarvestStatus.ERROR, e.getMessage());
        // there's no way resumption token is valid if we get here
        resource.setResumptionToken(null);
	logOaiPmhException(e, e.getMessage());
        logger.debug("Stack trace:", e);
      }
    } catch (IOException e) {
      //when we get here the retry loop has already been exhausted
      if (!isKillSent()) {
	setStatus(HarvestStatus.ERROR, e.getMessage());
	resource.setMessage(e.getMessage());
        // The resumption token
        if (resource.getClearRtOnError())
          resource.setResumptionToken(null);
	logger.log(Level.ERROR, e.getMessage());
	logger.debug("Stack trace:", e);
	return ;
      }
    } catch (Exception e) {
      if (isKillSent()) {
	logger.log(Level.INFO, "Shutting down.");
      }
      else {
	String msg  =  e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : "");
	logger.log(Level.ERROR, "Recieved " +  e.getClass().getSimpleName() + ": " + msg);
	logger.debug("Stack trace:", e);
	setStatus(HarvestStatus.ERROR, e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        if (resource.getClearRtOnError())
          resource.setResumptionToken(null);
        if (e instanceof StorageException) {
          logger.warn("StorageException: No attempt to commit/rollback changes");
          return;
        }
      }
    }
    // if there was no error we move the time marker
    if (getStatus() == HarvestStatus.OK || getStatus() == HarvestStatus.WARN) {
      try {
	String subject = "OAI harvest finished."; 
	commit();
	// TODO persist until and from, trash resumption token
	resource.setFromDate(resource.getUntilDate());
	resource.setUntilDate(null);
	resource.setResumptionToken(null);
	if (getStatus() == HarvestStatus.OK) /* Do not reset WARN state */ 
	  setStatus(HarvestStatus.FINISHED);
	String msg = "OAI harvest finished with status " + getStatus() + ". Next from: " + resource.getFromDate();
	logger.log(Level.INFO, subject + msg);
	mailMessage(subject, msg);
      } catch (Exception e) {
        String subject = "Storage commit failed: ";
        String errorMessage = e.getMessage();
	logError(subject, errorMessage);
	resource.setResumptionToken(startResumptionToken);
	mailMessage(subject, errorMessage);
      }
    } else {
      logger.warn("Terminated with non-OK status: Job status " + getStatus());
      // We do not want to override a ERROR message, but should reset a killed/running status. 
      // Perhaps even leave killed, just be sure that we will start the job in this state. 
      String subject = "OAI harvest stopped premature. ";
      String msg = ""; 
      boolean isError = false;
      if (getStatus().equals(HarvestStatus.KILLED) || getStatus().equals(HarvestStatus.RUNNING)) {
	msg = "Completed with status: " + getStatus().toString() + ". ";
	setStatus(HarvestStatus.FINISHED, msg);
      }
      else {
	setStatus(HarvestStatus.ERROR, msg);
	isError = true;
      }
      try {
	if (resource.getKeepPartial()) {
	  msg = msg + "Commiting up partial harvest as configured. ";
	  logger.log(Level.INFO, subject + msg);
	  commit();
	} else {
	  getStorage().rollback();
	  resource.setResumptionToken(startResumptionToken);
	  msg = msg + "Rolling back until "
	      + (startResumptionToken != null ? " resumptionToken (at start): "
		  + startResumptionToken : formatDate(resource.getFromDate()));
	  logger.log(Level.INFO, msg);

	}
	if (isError)
	  logError(subject, msg);
	mailMessage(subject, msg);
      } catch (Exception ioe) {
	msg = "Storage (partial) commit/rollback failed: " + ioe.getMessage();
	logger.debug("Stack trace:", ioe);
	logError(subject, msg);
	mailMessage(subject, msg);
	resource.setResumptionToken(startResumptionToken);
      }
    }
    } catch(Exception e) {
      logger.error("Unhandled Exception: " + e.getMessage());
    } finally {
      shutdown();
    }
  }

  protected void harvest(String baseURL, String from, String until, String metadataPrefix,
      String setSpec, String resumptionToken, RecordStorage storage, final DiskCache dc) throws 
      	TransformerException, IOException, ParserConfigurationException
      {

    ListRecords listRecords = new ListRecords(baseURL, proxy, logger.getLogger());
    listRecords.setHttpRetries(resource.getRetryCount());
    listRecords.setHttpTimeout(resource.getTimeout() );
    listRecords.setHttpRetryWait(resource.getRetryWait());
    if (!resource.isDiskRun() && resource.isCacheEnabled()) {
      listRecords.setInputStreamWrapper(new InputStreamWrapper() {
        @Override
        public InputStream wrap(InputStream is) throws IOException {
          return new CachingInputStream(is, joinPath(dc.getJobPath(), dc.proposeName()));
        }
      });
    }
    Queue<String> cacheQ = null;
    if (resource.isDiskRun()) {
      logger.log(Level.INFO, "OAI harvest restarted from the disk cache.");
      cacheQ = new LinkedList<String>(Arrays.asList(dc.list()));
      String next = cacheQ.poll();
      if (next != null) {
        logger.info("Processing cached response at '"+next+"'.");
        File nextF = new File(next);
        listRecords.harvest(new FileInputStream(nextF), resource.getEncoding(), (int) nextF.length());
      } else {
        logger.warn("Job disk cache is empty.");
      }
    } else if (resumptionToken == null || "".equals(resumptionToken)) {
      logger.log(Level.INFO, "OAI-PMH harvesting in " + metadataPrefix + " format from: "  
	  + formatDate(resource.getFromDate()) + " until: " + formatDate(resource.getUntilDate()) + ", date format used as shown.");
      listRecords.harvest(from, until, setSpec, metadataPrefix, proxy, resource.getEncoding());
    } else {
      logger.log(Level.INFO, "OAI harvest restarted using Resumption Token " + resource.getResumptionToken() + ".");
      listRecords.harvest(resumptionToken, proxy, resource.getEncoding());
    }

    boolean dataStart = false;
    int count = 0;
    while (!isKillSent()) {
      if (listRecords.getDocument() == null) {
	throw new OaiPmhException("Failed to parse response (empty document).", null);
      }
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
	  markForUpdate();
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
	  count = list.getLength();
	  totalCount += count;
	  if (totalCount % 1000 == 0) 
	    logger.info("Harvested " + totalCount + " records from " + baseURL); 
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
	  //e.printStackTrace();
	  throw e;
	}
      }
      if (!resource.isDiskRun() && resumptionToken == null || resumptionToken.length() == 0) {
	logger.log(Level.INFO, "" + count + " Records fetched. No resumptionToken received, harvest done.");
	setStatus(HarvestStatus.OK);
	break;
      } else {
	logger.log(Level.INFO, "" + count + " Records fetched, next resumptionToken is " + resumptionToken);
	resource.setResumptionToken(resumptionToken);
	markForUpdate();
	try {
          if (resource.isDiskRun()) {
            String next = cacheQ.poll();
            if (next != null) {
              logger.info("Processing cached response at '"+next+"'.");
              File nextF = new File(next);
              listRecords.harvest(new FileInputStream(nextF), 
                resource.getEncoding(), (int) nextF.length());
            } else {
              logger.info("No more files to process in the cache.");
              setStatus(HarvestStatus.OK);
              break;
            }
          } else {
            listRecords.harvest(resumptionToken, proxy, resource.getEncoding());
          }
	} catch (ResponseParsingException hve) {
	  String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
	  // dumping the response may cause IO Exception
	  logger.log(Level.DEBUG, msg + " Erroneous respponse:\n" + hve.getResponseString());
	  throw hve;
	  
	} catch (IOException io) {
	  throw io;
	} catch (Exception e) {
	  throw new IOException(e);
	}
      }
    }
    logger.info("Harvested " + totalCount + " records in total from " + baseURL); 
    if (dataStart)
      getStorage().databaseEnd();
  }

  protected void logOaiPmhException(OaiPmhException e, String string) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      Result xml = new StreamResult();
      transformer.transform(new DOMSource(e.getDocument()), xml);
      logger.error("Failed to get " + string + " from OAI-PMH XML response: " + xml.toString());
    } catch (TransformerConfigurationException e1) {
      logger.error("Failed to Transformer to serialize XML Document on error: " + e.getMessage());
      //e1.printStackTrace();
    } catch (TransformerFactoryConfigurationError e1) {
      logger.error("Failed to Transformer to serialize XML Document on error: " + e.getMessage());
      //e1.printStackTrace();
    } catch (TransformerException e1) {
      logger.error("Failed to serialize XML Document on error: " + e.getMessage());
      //e1.printStackTrace();
    }
  }

  protected RecordDOMImpl createRecord(Node node) throws TransformerException 
  {
    String id = HarvesterVerb.getSingleString(node, "./oai20:header/oai20:identifier/text()");
    String isDeleted = HarvesterVerb.getSingleString(node, "attribute::status"); 
    RecordDOMImpl record = new RecordDOMImpl(id, resource.getId().toString(), node);
    if ("deleted".equalsIgnoreCase(isDeleted)) 
      record.setDeleted(true);
    return record;
  }

  protected OAIError[] getErrors(NodeList errorNodes) {
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
  public Harvestable getHarvestable() {
    return resource;
  }

}