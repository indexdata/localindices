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
import ORG.oclc.oai.harvester2.verb.ListRecords;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.util.TextUtils;

/**
 * This class is an implementation of the OAI-PMH protocol and may be used by
 * the scheduler through the HarvestJob interface. This class updates some of
 * the Harvestable's properties excluding the STATUS, the status has to be
 * handled on the higher level.
 * 
 * @author jakub
 */
public class OAIHarvestJob extends AbstractHarvestJob {
  private StorageJobLogger logger; 
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

    @SuppressWarnings("unused")
    public String getMessage() {
      return message;
    }
  }

  public OAIHarvestJob(OaiPmhResource resource, Proxy proxy) {
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
    logger = new FileStorageJobLogger(this.getClass(), resource);
    
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    if (getStatus().equals(HarvestStatus.NEW) || getStatus().equals(HarvestStatus.ERROR))
      this.initialRun = true;
    // this.resource.setMessage(null);
  }

  @Override
  public void run() {
    setStatus(HarvestStatus.RUNNING);
    this.resource.setMessage(null);
    // figure out harvesting period, even though we may end up using
    // resumptionTokens from the DB
    Date nextFrom = null;
    if (resource.getUntilDate() != null)
      logger.log(Level.INFO, "OAI harvest: until param will be overwritten to yesterday.");
    resource.setUntilDate(yesterday());
    nextFrom = new Date();
    logger.log(Level.INFO, "OAI harvest started. Harvesting from: "
	+ resource.getFromDate() + " until: " + resource.getUntilDate());
    try {
      getStorage().begin();
      OutputStream out = getStorage().getOutputStream();

      // if (resource.getResumptionToken() != null) {
      // this is actually never called since we do not store resumption tokens
      // harvest(baseURL, resumptionToken, out);
      // } else {
      harvest(resource.getUrl(), formatDate(resource.getFromDate()),
	  formatDate(resource.getUntilDate()), resource.getMetadataPrefix(),
	  resource.getOaiSetName(), resource.getResumptionToken(), out);
      // }

    } catch (IOException e) {
      setStatus(HarvestStatus.ERROR);
      resource.setMessage(e.getMessage());
      logger.log(Level.DEBUG, e);
    }
    // if there was no error we move the time marker
    if (getStatus() != HarvestStatus.ERROR && getStatus() != HarvestStatus.KILLED) {
      // TODO persist until and from, trash resumption token
      resource.setFromDate(nextFrom);
      resource.setUntilDate(null);
      resource.setResumptionToken(null);
      setStatus(HarvestStatus.FINISHED);
      logger.log(Level.INFO, "OAI harvest finished OK. Next from: "
	  + resource.getFromDate());
      initialRun = false;
      try {
	// TODO Dont we wanna close the output?
	getStorage().commit();
      } catch (IOException ioe) {
	setStatus(HarvestStatus.ERROR);
	resource.setMessage(ioe.getMessage());
	logger.log(Level.ERROR, "Storage commit failed.");
      }
    } else {
      if (getStatus().equals(HarvestStatus.KILLED))
	setStatus(HarvestStatus.FINISHED);
      logger.log(Level.INFO, "OAI harvest killed/faced error "
	  + "- rolling back. Next from param: " + resource.getFromDate());
      try {
	getStorage().rollback();
      } catch (IOException ioe) {
	logger.log(Level.ERROR, "Storage rollback failed.");
      }
    }
  }

  private void harvest(String baseURL, String from, String until, String metadataPrefix,
      String setSpec, String resumptionToken, OutputStream out) throws IOException {
    ListRecords listRecords = null;
    // resumption Token present in DB?
    if (resumptionToken == null || "".equals(resumptionToken)) {
      listRecords = listRecords(baseURL, from, until, setSpec, metadataPrefix);
    } else {
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
      OAIError[] errors = null;
      if ((errors = getErrors(errorNodes)) != null) {
	// the error msg has been logged, but print out the full record
	logger.log(Level.DEBUG,
	    "OAI error response: \n" + listRecords.toString());
	// if this is noRecordsMatch and inital run, something is wrong
	if (errors.length == 1 && errors[0].getCode().equalsIgnoreCase("noRecordsMatch")
	    && !this.initialRun) {
	  logger.log(Level.INFO, "Response noRecordsMatch experienced for non-initial harvest - ignoring");
	  setStatus(HarvestStatus.KILLED);
	  return;
	} else
	  throw new IOException("OAI error " + errors[0].code + ": " + errors[0].message);
      } else {
	if (!dataStart) {
	  out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
	  out.write(("<harvest from=\"" + from + "\" until=\"" + until + "\">\n").getBytes("UTF-8"));
	  dataStart = true;
	}
	out.write(listRecords.toString().getBytes("UTF-8"));
	out.write("\n".getBytes("UTF-8"));
	try {
	  resumptionToken = listRecords.getResumptionToken();
	} catch (Exception e) {
	  throw new IOException("Cannot read the resumption token");
	}
      }
      if (resumptionToken == null || resumptionToken.length() == 0) {
	logger.log(Level.INFO, "Records stored. No resumptionToken received, harvest done.");
	break;
      } else {
	logger.log(Level.INFO, "Records stored, next resumptionToken is " + resumptionToken);
	resource.setResumptionToken(resumptionToken);
	markForUpdate();
	listRecords = listRecords(baseURL, resumptionToken);
      }
    }
    if (dataStart)
      out.write("</harvest>\n".getBytes("UTF-8"));
  }

  private ListRecords listRecords(String baseURL, String from, String until, String setSpec,
      String metadataPrefix) throws IOException {
    try {
      return new ListRecords(baseURL, from, until, setSpec, metadataPrefix, proxy, resource.getEncoding(), logger.getLogger());
    } catch (ResponseParsingException hve) {
      String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
      logger.log(Level.DEBUG, msg + " Erroneous response:\n"
	  + TextUtils.readStream(hve.getResponseStream()));
      throw new IOException(msg, hve);
    } catch (IOException io) {
      throw io;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private ListRecords listRecords(String baseURL, String resumptionToken) throws IOException {
    try {
      return new ListRecords(baseURL, resumptionToken, proxy, resource.getEncoding(), logger.getLogger());
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
	logger.log(Level.WARN, "OAI harvest error - " + code + ": "
	    + message);
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

  private String formatDate(Date date) {
    if (date == null)
      return null;
    return new SimpleDateFormat(currentDateFormat).format(date);
  }

  @Override
  public OutputStream getOutputStream() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }
}