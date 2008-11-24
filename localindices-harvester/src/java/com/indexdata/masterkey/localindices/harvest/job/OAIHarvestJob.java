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
import java.util.Calendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
    private boolean die = false;
    private final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private String currentDateFormat;

    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.INFO, "OAI harvest thread received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }

    public OAIHarvestJob(OaiPmhResource resource) {
        if (resource.getUrl() == null) {
            throw new IllegalArgumentException("baseURL parameter cannot be null");
        }
        if (resource.getMetadataPrefix() == null || resource.getMetadataPrefix().isEmpty()) {
            resource.setMetadataPrefix("oai_dc");
        }
        if (resource.getOaiSetName() != null && resource.getOaiSetName().isEmpty()) {
            resource.setOaiSetName(null);
        }
        if (resource.getDateFormat() != null) {
            currentDateFormat = resource.getDateFormat();
        } else {
            currentDateFormat = DEFAULT_DATE_FORMAT;
        }
        this.resource = resource;     
        String persistedStatus = resource.getCurrentStatus();
        if (persistedStatus == null)
            this.status = HarvestStatus.NEW;
        else
            this.status = HarvestStatus.WAITING;
        this.resource.setError(null);
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
        logger.log(Level.INFO, "OAI harvest received finish notification.");
        if (status.equals(HarvestStatus.FINISHED)) {
            status = HarvestStatus.WAITING;
        }
        logger.log(Level.INFO, "OAI harvest job's status after finish: " + status);
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
            logger.log(Level.INFO, "OAI harvest: until param will be overwritten to yesterday.");
        resource.setUntilDate(yesterday());
        nextFrom = new Date();
        
        logger.log(Level.INFO, "OAI harvest started. Harvesting from: " 
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
            resource.setError(e.getMessage());
            logger.log(Level.ERROR, e);
        }
        // if there was an error do not move the time marker
        // - we'll try havesting data next time
        if (status != HarvestStatus.ERROR && status != HarvestStatus.KILLED) {
            //TODO persist until and from
            resource.setFromDate(nextFrom);
            resource.setUntilDate(null);
            status = HarvestStatus.FINISHED;
            logger.log(Level.INFO, "OAI harvest finishes OK. Next from: " 
                    + resource.getFromDate());
            try {
                storage.commit();
            } catch (IOException ioe) {
                status = HarvestStatus.ERROR;
                resource.setError(ioe.getMessage());
                logger.log(Level.ERROR, "Storage commit failed.");
            }
        } else {
            logger.log(Level.INFO, "OAI harvest killed/faced error " +
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
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
        out.write(("<harvest from=\"" + from + "\" until=\"" + until + "\">\n").getBytes("UTF-8"));
        //out.write(new Identify(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListMetadataFormats(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        //out.write(new ListSets(baseURL).toString().getBytes("UTF-8"));
        //out.write("\n".getBytes("UTF-8"));
        ListRecords listRecords = null;
        try {
            listRecords = new ListRecords(baseURL, from, until, setSpec,
                metadataPrefix);
        } catch (HarvesterVerbException hve) {
            logger.log(Level.ERROR, "ListRecords failed, invalid XML response:\n"
                    + TextUtils.readStream(hve.getResponseStream()));
            throw new IOException("ListRecords failed because of the invalid XML", hve);
        } catch (Exception e) {
            throw new IOException(e);
        }
        while (listRecords != null && !isKillSendt()) {
            NodeList errors = null;
            try {
                errors = listRecords.getErrors();
            } catch (TransformerException te) {
                throw new IOException("Cannot read OAI-PMH protocol errors.", te);
            }
            if (checkError(errors)) {
                logger.log(Level.ERROR, "Error record: " + listRecords.toString());
                break;
            }
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            String resumptionToken = null;
            try {
                resumptionToken = listRecords.getResumptionToken();
            } catch (Exception e) {
                throw new IOException("Cannot read the resumption token");
            }
            if (resumptionToken == null || resumptionToken.length() == 0) {
                logger.log(Level.INFO, "Records stored. No resumptionToken received, harvest done.");
                listRecords = null;
            } else {
                logger.log(Level.INFO, "Records stored, next resumptionToken is " + resumptionToken);
                try {
                    listRecords = new ListRecords(baseURL, resumptionToken);
                } catch (HarvesterVerbException hve) {
                  throw new IOException("ListRecords failed because of the invalid XML", hve);
                } catch (Exception e) {
                  throw new IOException(e);
                }
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }
    
    private void harvest(String baseURL, String resumptionToken,
            OutputStream out)
            throws IOException, ParserConfigurationException, HarvesterVerbException, TransformerException,
            NoSuchFieldException {
        ListRecords listRecords = new ListRecords(baseURL, resumptionToken);
        while (listRecords != null && !isKillSendt()) {
            NodeList errors = listRecords.getErrors();
            if (checkError(errors)) {
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

    private boolean checkError(NodeList errors) {
        if (errors != null && errors.getLength() > 0) {
            status = HarvestStatus.ERROR;
            int length = errors.getLength();
            String error = "";
            for (int i = 0; i < length; ++i) {
                Node item = errors.item(i);
                error += item.getTextContent();
            }
            resource.setError(error);
            logger.log(Level.ERROR, "OAI job's error: " + error);
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
        return new SimpleDateFormat(currentDateFormat).format(date);
    }


}
