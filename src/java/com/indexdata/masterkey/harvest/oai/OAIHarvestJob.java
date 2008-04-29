package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

/* package ; */

// import java.io.*;
// import java.lang.NoSuchFieldException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.HashMap;
// import javax.xml.parsers.ParserConfigurationException;
// import javax.xml.transform.TransformerException;
// import org.xml.sax.SAXException;

import java.io.File;
import java.util.Date;
import ORG.oclc.oai.harvester2.verb.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
//import org.apache.log4j.Logger;

public class OAIHarvestJob extends HarvestJob {

    public static final String JOB_TYPE = "OAI"; 

    public OAIHarvestJob (String baseURL,
                          String from,
                          String until,
                          String metadataPrefix,
                          String setSpec) {
        this.baseURL = baseURL;
        this.from = from;
        this.until = until;
        this.metadataPrefix = metadataPrefix;
        this.setSpec = setSpec;

        if (this.baseURL == null) 
            throw new IllegalArgumentException("OAIHarvestJob: " 
                                               + "expect 'baseURL' parameter");

        if (this.from == null) 
            throw new IllegalArgumentException("OAIHarvestJob: "
                                               + "expect 'from' parameter");

        if (this.until == null) 
            throw new IllegalArgumentException("OAIHarvestJob: "
                                               + "expect 'until' parameter");

        if (this.metadataPrefix == null) 
            this.metadataPrefix = "oai_dc";

        //if (this.setSpec == null) 
        //  throw new IllegalArgumentException("OAIHarvestJob: "
        //                                     + "expect 'setSpec' parameter");

        this.job = this.from + "+" + this.until 
            + "+" + this.metadataPrefix + "+" + this.setSpec;

        this.resumptionToken = null;

        this.status = STATUS_NEW;
    }

    // HarvestJob overrides

    public String baseURL() { return this.baseURL; }
    public String job() { return this.job; }
    public String part() {
        if (this.part == "ListRecords") {
            return this.part + File.separatorChar + this.job; 
        }
        return this.part; 
    }
    public String batch() { return this.resumptionToken; }
    public String status() { return this.status; }
    public String type() { return JOB_TYPE; }

    public void run() 
    //throws IOException, ParserConfigurationException, SAXException, 
    //           TransformerException, NoSuchFieldException
    {
        try {
            //OutputStream os = System.out;

            // Before the job starts
            logger.info(message());
            this.status = STATUS_ACTIVE;
    
            this.storage.store(this, "test");

            // Identify
            {
                if (this.status != STATUS_ACTIVE) return; 
                this.part = "Identify";
                logger.info(message());
                    //.toString().getBytes("UTF-8");
                Identify identify = new Identify(baseURL);

                if (identify != null) {
                    NodeList errors = identify.getErrors();
                    if (got_error(errors)) {
                        this.status = STATUS_ERROR;
                        // logger.info(error_message(errors));
                        logger.info(identify.toString());
                        //break;
                    } 
                    this.storage.store(this, identify.toString());
                }
                else {
                    this.status = STATUS_ERROR;
                    logger.info(message());
                    return;
                }
            }

            // ListMetadataFormats
            {
                if (this.status != STATUS_ACTIVE) return; 
                this.part = "ListMetadataFormats";
                logger.info(message());
                ListMetadataFormats meta = new ListMetadataFormats(baseURL);

                if (meta != null) {
                    NodeList errors = meta.getErrors();
                    if (got_error(errors)) {
                        this.status = STATUS_ERROR;
                        // logger.info(error_message(errors));
                        logger.info(meta.toString());
                    } 
                    this.storage.store(this, meta.toString());
                }
                else {
                    this.status = STATUS_ERROR;
                    logger.info(message());
                    return;
                }
            }

            // ListSets
            {
                if (this.status != STATUS_ACTIVE) return; 
                this.part = "ListSets";
                logger.info(message());
                ListSets sets = new ListSets(this.baseURL);

                // while (sets != null) {0
                if (sets != null) {
                    NodeList errors = sets.getErrors();
                    if (got_error(errors)) {
                        this.status = STATUS_ERROR; 
                        logger.info(message());
                        // logger.info(error_message(errors));
                        logger.info(sets.toString());
                        // some servers don't support ListSets, that's OK
                        this.status = STATUS_ACTIVE; 
                        //break;
                    }
                    
                    this.storage.store(this, sets.toString());

                    // the current implementation of the OCLC OAI libs does
                    // not allow for resumption tokens during ListSets, as
                    // they should according to the OAI protocol. 

                    //checking for resumption tokens and next batch
                    //this.resumptionToken = sets.getResumptionToken();
                    //if (this.resumptionToken == null 
                    //   || this.resumptionToken.length() == 0) {
                    //    this.resumptionToken = null; 
                    //   sets = null;
                    //} else {
                    // logger.info(message());
                    //   //sets = new ListSets(baseURL, this.resumptionToken);
                    //   sets = new ListSets(baseURL);
                    //}
                }
                else {
                    this.status = STATUS_ERROR;
                    logger.info(message());
                    return;
                }
            }


            // ListIdentifiers - not done, we want the data !!

            // ListRecords
            {
                if (this.status != STATUS_ACTIVE) return; 
                this.part = "ListRecords";
                logger.info(message());
                ListRecords records 
                    = new ListRecords(this.baseURL, 
                                      this.from, this.until,
                                      this.setSpec, this.metadataPrefix);

                while (records != null) {
                    NodeList errors = records.getErrors();
                    if (got_error(errors)) {
                        this.status = STATUS_ERROR;
                        logger.info(message());
                        // logger.info(error_message(errors));
                        // logger.info(records.toString());

                        this.storage.store(this, records.toString());

                        break;
                    } 
                    
                    this.storage.store(this, records.toString());


                    // checking for resumption tokens and next batch
                    this.resumptionToken = records.getResumptionToken();
                    if (this.resumptionToken == null 
                        || this.resumptionToken.length() == 0) {
                        this.resumptionToken = null; 
                        records = null;
                    } else {
                        logger.info(message());
                        records = new ListRecords(baseURL, 
                                                  this.resumptionToken);
                    }   
                }
            }
           
            // we got through the entire harvesting without problems
            this.end = new Date();
            this.resumptionToken = null; 
            this.status = STATUS_FINISHED;
            logger.info(message());


            // Exceptions which need additional handling are:
            // IOException, ParserConfigurationException, SAXException, 
            // TransformerException, NoSuchFieldException

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        
        } catch (Error e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    



    // OAIHarvestJob private things

    private boolean got_error(NodeList errors){
         if (errors != null && errors.getLength() > 0) {
             this.status = STATUS_ERROR;
             return true;
         }
         return false;
    }               
         
    private String error_message(NodeList errors){

         if (errors != null && errors.getLength() > 0) {
             this.status = STATUS_ERROR;
                        
             logger.info(message());
             
             int length = errors.getLength();
             for (int i=0; i<length; ++i) {
                 Node item = errors.item(i);
                 logger.log(Level.INFO, item.getTextContent());
             }
         }
         return "error";
    }

    private static Logger logger = Logger.getLogger(OAIHarvestJob.class.getCanonicalName());
   
    private String baseURL = null;
    private String from = null;
    private String until = null;
    private String metadataPrefix = null;
    private String resumptionToken = null;
    private String setSpec = null;

    private Date start = new Date();
    private Date end = null;

    private String job = null;
    private String part = null;
    private String status = null;

}
