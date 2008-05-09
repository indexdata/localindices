package com.indexdata.masterkey.harvest.oai;

/**
Copyright 2008 Index Data ApS
http://www.indexdata.com
Licensed under the GNU Public License, Version 2.0.
 */
import ORG.oclc.oai.harvester2.verb.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.indexdata.localindexes.web.entity.OaiPmhResource;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * This class was rewritten and now follows OCLC's RawWrite.java
 * @author jakub
 */
public class OAIHarvestJob implements HarvestJob {

    private String baseURL;
    private String from;
    private String until;
    private String metadataPrefix;
    private String setSpec;
    private String resumptionToken;
    private HarvestStatus status;
    private HarvestStorage storage;
    private static Logger logger;
    private boolean die = false;
    private String error;

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
        this(resource.getUrl(),
                null,
                null,
                resource.getMetadataPrefix(),
                resource.getOaiSetName());
    }

    public OAIHarvestJob(String baseURL, String from, String until, String metadataPrefix, String setSpec) {
        if (baseURL == null) {
            throw new IllegalArgumentException("baseURL parameter cannot be null");
        }
        if (metadataPrefix == null) {
            metadataPrefix = "oai_dc";
        }
        
        this.baseURL = baseURL;
        this.metadataPrefix = metadataPrefix;
        this.from = from;
        this.until = until;
        this.setSpec = setSpec;

        status = HarvestStatus.NEW;
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    public void kill() {
        status = HarvestStatus.KILLED;
        onKillSendt();
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
        return error;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public void run() {
        logger.log(Level.INFO, Thread.currentThread().getName() + ": OAI harvest thread started.");
        status = HarvestStatus.RUNNING;
        try {
            //calculate harvesting period
            if (until == null)
                until = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            else
                logger.log(Level.INFO, Thread.currentThread().getName() 
                        + ": OAI harvest thread: something is wrong with the time marker, from: ."
                        + from + " until: " + until);

            OutputStream out = storage.getOutputStream();

            if (resumptionToken != null) {
                // this is actually never called since we do not store resumption tokens
                harvest(baseURL, resumptionToken, out);
            } else {
                harvest(baseURL, from, until, metadataPrefix, setSpec, out);
            }
            if (out != System.out) {
                out.close();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, Thread.currentThread().getName(), e);
        }

        logger.log(Level.INFO, Thread.currentThread().getName() + ": OAI harvest thread finishes.");
        // clean-up when killed
        // move the time marker
        if (status != HarvestStatus.ERROR && status != HarvestStatus.KILLED) {
                from = until;
                until = null;
        }
        if (status != HarvestStatus.ERROR) {
            status = HarvestStatus.FINISHED;
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
        out.write("<harvest>\n".getBytes("UTF-8"));
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
            error = null;
            for (int i = 0; i < length; ++i) {
                Node item = errors.item(i);
                error += item.getTextContent();
            }
            logger.log(Level.SEVERE, Thread.currentThread().getName() + ": OAI harvest error: " + error);
            return true;
        }
        return false;
    }
}
