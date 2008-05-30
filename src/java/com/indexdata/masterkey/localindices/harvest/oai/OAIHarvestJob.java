/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.oai;

import ORG.oclc.oai.harvester2.verb.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * This class was rewritten and now follows OCLC's RawWrite.java
 * @author jakub
 */
public class OAIHarvestJob implements HarvestJob {
    private OaiPmhResource resource;
    /*private String baseURL;
    private String from;
    private String until;
    private String metadataPrefix;
    private String setSpec;
    private String resumptionToken;
    private String error;
     */
    //get rid of this field
    private HarvestStatus status;
    private HarvestStorage storage;
    private static Logger logger;
    private boolean die = false;
    private final static String DATE_FORMAT = "yyyy-MM-dd";

    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.INFO, Thread.currentThread().getName() + ": OAI harvest thread received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }

    public OAIHarvestJob(OaiPmhResource resource) {
        logger = Logger.getLogger(this.getClass().getCanonicalName());        
        if (resource.getUrl() == null) {
            throw new IllegalArgumentException("baseURL parameter cannot be null");
        }
        if (resource.getMetadataPrefix() == null) {
            resource.setMetadataPrefix("oai_dc");
        }
        this.resource = resource;        
        String persistedStatus = resource.getCurrentStatus();
        if (persistedStatus == null
             || persistedStatus.equals("RUNNING"))
            this.status = HarvestStatus.NEW;
        else
            this.status = HarvestStatus.valueOf(persistedStatus);       
    }

    public void kill() {
        if (status != HarvestStatus.FINISHED) {
            status = HarvestStatus.KILLED;
            onKillSendt();
        }
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void finishReceived() {
        logger.log(Level.INFO, Thread.currentThread().getName() + ": OAI harvest thread received finish notification.");
        if (status.equals(HarvestStatus.FINISHED)) {
            status = HarvestStatus.WAITING;
        }
        logger.log(Level.INFO, Thread.currentThread().getName() + ": OAI harvest thread status: " + status);
    }

    public String getError() {
        return resource.getError();
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }
    
    public HarvestStorage getStorage() {
        return storage;
    }
    
    public void run() {
        // where are we?
        Date nextFrom = null;
        if (resource.getUntilDate() != null)
            logger.log(Level.SEVERE, Thread.currentThread().getName() 
                    + " until param will be overwritten to yesterday.");
        resource.setUntilDate(yesterday());
        nextFrom = new Date();
        
        logger.log(Level.INFO, Thread.currentThread().getName() 
                + ": OAI harvest thread started. Harvesting from: " 
                + resource.getFromDate() + " until: " + resource.getUntilDate());
        
        status = HarvestStatus.RUNNING; 
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
            logger.log(Level.SEVERE, Thread.currentThread().getName(), e);
        }
        // if there was an error do not move the time marker
        // - we'll try havesting data next time
        if (status != HarvestStatus.ERROR && status != HarvestStatus.KILLED) {
            //TODO persist until and from
            resource.setFromDate(nextFrom);
            resource.setUntilDate(null);
            status = HarvestStatus.FINISHED;
            logger.log(Level.INFO, Thread.currentThread().getName() 
                    + ": OAI harvest thread finishes OK. Next from: " 
                    + resource.getFromDate());
            try {
                storage.commit();
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Storage commit failed.");
            }
        } else {
            logger.log(Level.INFO, Thread.currentThread().getName() 
                    + ": OAI harvest thread killed/faced error " +
                    "- rolling back. Next from param: " + resource.getFromDate());
            try {
                storage.rollback();
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Storage rollback failed.");
            }            
        }
    }

    private void harvest(String baseURL, String resumptionToken,
            OutputStream out)
            throws IOException, ParserConfigurationException, SAXException, TransformerException,
            NoSuchFieldException {
        ListRecords listRecords = new ListRecords(baseURL, resumptionToken);
        while (listRecords != null && !isKillSendt()) {
            NodeList errors = listRecords.getErrors();
            if (checkError(errors)) {
                logger.log(Level.SEVERE, Thread.currentThread().getName() + ": Error record: " + listRecords.toString());
                break;
            }
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            resumptionToken = listRecords.getResumptionToken();
            logger.log(Level.INFO, Thread.currentThread().getName() + ": next resumptionToken: " + resumptionToken);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
            } else {
                listRecords = new ListRecords(baseURL, resumptionToken);
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }

    private void harvest(String baseURL, String from, String until,
            String metadataPrefix, String setSpec,
            OutputStream out)
            throws IOException, ParserConfigurationException, SAXException, TransformerException,
            NoSuchFieldException {
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
        out.write(("<harvest from=\"" + from + "\" until=\"" + until + "\">\n").getBytes("UTF-8"));
        //out.write(new Identify(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListMetadataFormats(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListSets(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        ListRecords listRecords = new ListRecords(baseURL, from, until, setSpec,
                metadataPrefix);
        while (listRecords != null && !isKillSendt()) {
            NodeList errors = listRecords.getErrors();
            if (checkError(errors)) {
                logger.log(Level.SEVERE, Thread.currentThread().getName() + ": Error record: " + listRecords.toString());
                break;
            }
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            String resumptionToken = listRecords.getResumptionToken();
            if (resumptionToken == null || resumptionToken.length() == 0) {
                logger.log(Level.INFO, Thread.currentThread().getName() + ": Records stored. No resumptionToken received, harvest done.");
                listRecords = null;
            } else {
                logger.log(Level.INFO, Thread.currentThread().getName() + ": Records stored, next resumptionToken is " + resumptionToken);
                listRecords = new ListRecords(baseURL, resumptionToken);
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }

    private boolean checkError(NodeList errors) {
        if (errors != null && errors.getLength() > 0) {
            status = HarvestStatus.ERROR;
            int length = errors.getLength();
            String error = null;
            for (int i = 0; i < length; ++i) {
                Node item = errors.item(i);
                error += item.getTextContent();
            }
            resource.setError(error);
            logger.log(Level.SEVERE, Thread.currentThread().getName() + ": OAI harvest error: " + error);
            return true;
        }
        return false;
    }

    private Date yesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1); //back one
        return c.getTime();
    }
    
    private String formatDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }


}
