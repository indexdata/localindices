package com.indexdata.masterkey.harvest.oai;

/**
Copyright 2008 Index Data ApS
http://www.indexdata.com
Licensed under the GNU Public License, Version 2.0.
 */
import ORG.oclc.oai.harvester2.verb.*;
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
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }

    public OAIHarvestJob(OaiPmhResource resource) {
        this(resource.getUrl(), 
                "2008-03-01", 
                "2008-05-05", 
                resource.getMetadataPrefix(), 
                resource.getOaiSetName()
                );
    }

    public OAIHarvestJob(String baseURL, String from, String until, String metadataPrefix, String setSpec) {

        logger = Logger.getLogger(this.getClass().getCanonicalName());

        this.baseURL = baseURL;
        this.from = from;
        this.until = until;
        this.metadataPrefix = metadataPrefix;
        this.setSpec = setSpec;

        if (this.baseURL == null) {
            throw new IllegalArgumentException("baseURL parameter cannot be null");
        }
        if (this.from == null) {
            throw new IllegalArgumentException("from parameter cannot be null");
        }
        if (this.until == null) {
            throw new IllegalArgumentException("'until parameter cannot be null");
        }

        if (this.metadataPrefix == null) {
            this.metadataPrefix = "oai_dc";
        }

        status = HarvestStatus.NEW;
    }

    public void kill() {
        status = HarvestStatus.KILLED;
        onKillSendt();
    }

    public HarvestStatus getStatus() {
        return status;
    }
    
    public void finishReceived() {
        if (status.equals(HarvestStatus.FINISHED))
            status = HarvestStatus.WAITING;
    }

    public String getError() {
        return error;
    }
    
    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public void run() {
        logger.log(Level.INFO, "OAI harvest thread started.");
        status = HarvestStatus.RUNNING;
        try {
            OutputStream out = storage.getOutputStream();

            if (resumptionToken != null) {
                harvest(baseURL, resumptionToken, out);
            } else {
                harvest(baseURL, from, until, metadataPrefix, setSpec, out);
            }
            if (out != System.out) {
                out.close();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        }

        logger.log(Level.INFO, "OAI harvest thread finishes.");
        if (status != HarvestStatus.ERROR)
            status = HarvestStatus.FINISHED;        
    }

    private void harvest(String baseURL, String resumptionToken,
            OutputStream out)
            throws IOException, ParserConfigurationException, SAXException, TransformerException,
            NoSuchFieldException {
        ListRecords listRecords = new ListRecords(baseURL, resumptionToken);
        while (listRecords != null || !isKillSendt()) {
            if (checkError(listRecords)) break;
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            resumptionToken = listRecords.getResumptionToken();
            System.out.println("resumptionToken: " + resumptionToken);
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
        out.write(new Identify(baseURL).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        out.write(new ListMetadataFormats(baseURL).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        out.write(new ListSets(baseURL).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        ListRecords listRecords = new ListRecords(baseURL, from, until, setSpec,
                metadataPrefix);
        while (listRecords != null || !isKillSendt()) {
            if (checkError(listRecords)) break;
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            String resumptionToken = listRecords.getResumptionToken();
            System.out.println("resumptionToken: " + resumptionToken);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
            } else {
                listRecords = new ListRecords(baseURL, resumptionToken);
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }
    
    private boolean checkError(ListRecords listRecords) throws TransformerException {
        NodeList errors = listRecords.getErrors();
        if (errors != null && errors.getLength() > 0) {
            System.out.println("Found errors");
            status = HarvestStatus.ERROR;
            int length = errors.getLength();
            error = null;
            for (int i = 0; i < length; ++i) {
                Node item = errors.item(i);
                error += item.getTextContent();
                System.out.println(item);
            }
            System.out.println("Error record: " + listRecords.toString());
            return true;
        }
        return false;
    }
}
