/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import ORG.oclc.oai.harvester2.verb.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.util.TextUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.util.Calendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.log4j.Priority;

/**
 * This class is an implementation of the OAI-PMH protocol and may be used
 * by the scheduler through the HarvestJob interface. This class updates some of
 * the Harvestable's properties excluding the STATUS, the status has to be handled
 * on the higher level.
 * 
 * @author jakub
 */
public class OAIHarvestJob implements HarvestJob {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private OaiPmhResource resource;
    private HarvestStatus status;
    private HarvestStorage storage;
    private Proxy proxy;
    private boolean die = false;
    private final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private String currentDateFormat;
    private final static int MAX_ERROR_RETRY = 3; // If this is > 0, harvester will retry requests on errorNodes
    private final static int ERROR_SLEEP = 60 * 1000; // Sleep for awhile if there is an error
    private boolean initialRun = true;

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

        public String getMessage() {
            return message;
        }

    }

    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.INFO, "JOB#"+resource.getId()+
                    " OAI harvest thread received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
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
        this.status = HarvestStatus.valueOf(resource.getCurrentStatus());
        if (this.status.equals(HarvestStatus.NEW) || this.status.equals(HarvestStatus.ERROR))
            this.initialRun = true;
        //this.resource.setMessage(null);
    }

    public void kill() {
        if (status == HarvestStatus.RUNNING) {
            status = HarvestStatus.KILLED;
            onKillSendt();
        }
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void finishReceived() {
        logger.log(Level.INFO, "JOB#"+resource.getId()+ 
                " OAI harvest received finish notification.");
        if (status.equals(HarvestStatus.FINISHED)) {
            status = HarvestStatus.WAITING;
        }
        logger.log(Level.INFO, "JOB#"+resource.getId()+
                " OAI harvest job's status after finish: " + status);
    }

    public String getError() {
        return resource.getMessage();
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }
    
    public HarvestStorage getStorage() {
        return storage;
    }
    
    public void run() {
        status = HarvestStatus.RUNNING;
        this.resource.setMessage(null);
        // where are we?
        Date nextFrom = null;
        if (resource.getUntilDate() != null)
            logger.log(Level.INFO, "JOB#"+resource.getId()+
                    " OAI harvest: until param will be overwritten to yesterday.");
        resource.setUntilDate(yesterday());
        nextFrom = new Date();        
        logger.log(Level.INFO, "JOB#"+resource.getId()+ " OAI harvest started. Harvesting from: "
                + resource.getFromDate() + " until: " + resource.getUntilDate());        
        try {
            storage.begin();
            OutputStream out = storage.getOutputStream();
            
            //if (resource.getResumptionToken() != null) {
                // this is actually never called since we do not store resumption tokens
            //    harvest(baseURL, resumptionToken, out);
            //} else {
                harvest(resource.getUrl(), 
                        formatDate(resource.getFromDate()), 
                        formatDate(resource.getUntilDate()), 
                        resource.getMetadataPrefix(), 
                        resource.getOaiSetName(),
                        out);
            //}
            
        } catch (Exception e) {
            status = HarvestStatus.ERROR;
            resource.setMessage(e.getMessage());
            logger.log(Level.DEBUG, e);
        }
        // if there was no error we move the time marker
        if (status != HarvestStatus.ERROR && status != HarvestStatus.KILLED) {
            //TODO persist until and from
            resource.setFromDate(nextFrom);
            resource.setUntilDate(null);
            status = HarvestStatus.FINISHED;
            logger.log(Level.INFO, "JOB#"+resource.getId()+
                    " OAI harvest finished OK. Next from: "
                    + resource.getFromDate());
            initialRun = false;
            try {
                storage.commit();
            } catch (IOException ioe) {
                status = HarvestStatus.ERROR;
                resource.setMessage(ioe.getMessage());
                logger.log(Level.ERROR, "Storage commit failed.");
            }
        } else {
            if (status.equals(HarvestStatus.KILLED)) status = HarvestStatus.FINISHED;
            logger.log(Level.INFO, "JOB#"+resource.getId()+" OAI harvest killed/faced error " +
                    "- rolling back. Next from param: " + resource.getFromDate());
            try {
                storage.rollback();
            } catch (IOException ioe) {
                logger.log(Level.ERROR, "Storage rollback failed.");
            }            
        }
    }

    private void harvest(String baseURL, String from, String until,
            String metadataPrefix, String setSpec,
            OutputStream out) throws IOException {
        //out.write(new Identify(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListMetadataFormats(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListSets(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        ListRecords listRecords = null;
        try {
            listRecords = new ListRecords(baseURL, from, until, setSpec,
                metadataPrefix, proxy);
        } catch (HarvesterVerbException hve) {
            String msg = "ListRecords (" + hve.getRequestURL() + ") failed. "
                    + hve.getMessage();
            logger.log(Level.DEBUG, "JOB#"+resource.getId() + msg + " Erroneous respponse:\n"
                    + TextUtils.readStream(hve.getResponseStream()));
            throw new IOException(msg, hve);
        } catch (Exception e) {
            throw new IOException(e);
        }

        boolean dataStart = true;
        String resumptionToken = null;
        while (listRecords != null && !isKillSendt()) {
            NodeList errorNodes = null;
            try {
                errorNodes = listRecords.getErrors();
            } catch (TransformerException te) {
                throw new IOException("Cannot read OAI-PMH protocol errors.", te);
            }
            OAIError[] errors = null;
            if ((errors = getErrors(errorNodes)) != null) {
                //the error msg has been logged, but print out the full record
                logger.log(Level.DEBUG, "JOB#"+resource.getId()+" OAI error response: \n"
                        + listRecords.toString());
                //if this is noRecordsMatch and inital run, something is wrong
                if (errors.length == 1 &&
                    errors[0].getCode().equalsIgnoreCase("noRecordsMatch") &&
                    !this.initialRun) {
                    logger.log(Level.INFO, "JOB#"+resource.getId()+
                            " noRecordsMatch experienced for non-initial harvest - ignoring");
                    status = HarvestStatus.KILLED;
                    return;
                } else throw new IOException("OAI error "
                        + errors[0].code + ": " + errors[0].message);
            } else {
                if (dataStart) {
                    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
                    out.write(("<harvest from=\"" + from + "\" until=\"" + until + "\">\n").getBytes("UTF-8"));
                    dataStart = false;
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
                try {
                    listRecords = new ListRecords(baseURL, resumptionToken, proxy);
                } catch (HarvesterVerbException hve) {
                    String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " 
                            + hve.getMessage();
                    logger.log(Level.ERROR, msg + " Erroneous respponse:\n" 
                            + TextUtils.readStream(hve.getResponseStream()));
                    throw new IOException(msg, hve);
                } catch (Exception e) {
                  throw new IOException(e);
                }
            }
            
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }

    /*

    private void harvest(String baseURL, String resumptionToken,
            OutputStream out)
            throws IOException, ParserConfigurationException, HarvesterVerbException, TransformerException,
            NoSuchFieldException {
        ListRecords listRecords = new ListRecords(baseURL, resumptionToken);
        while (listRecords != null && !isKillSendt()) {
            NodeList errorNodes = listRecords.getErrors();
            if (getErrors(errorNodes)) {
                logger.log(Level.ERROR, "OAI job's error record: " + listRecords.toString());
                break;
            }
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            resumptionToken = listRecords.getResumptionToken();
            logger.log(Level.INFO, "OAI job's next resumptionToken: " + resumptionToken);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
            } else {
                listRecords = new ListRecords(baseURL, resumptionToken);
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }


     */

    private OAIError[] getErrors(NodeList errorNodes) {
        if (errorNodes != null && errorNodes.getLength() > 0) {
            int length = errorNodes.getLength();
            OAIError[] errors = new OAIError[length];
            for (int i = 0; i < length; ++i) {
                Node item = errorNodes.item(i);
                String code = item.getAttributes().getNamedItem("code").getNodeValue();
                String message = item.getTextContent();
                errors[i] = new OAIError(code, message);
                logger.log(Level.WARN, "JOB#"+resource.getId() + " OAI harvest error - " +
                        code + ": " + message);
            }
            return errors;
        }
        return null;
    }

    private Date yesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1); //back one
        return c.getTime();
    }
    
    private String formatDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(currentDateFormat).format(date);
    }


}
