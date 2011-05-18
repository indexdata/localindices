/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.ListRecords;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.util.TextUtils;

/**
 * This class is an implementation of the OAI-PMH protocol and may be used
 * by the scheduler through the HarvestJob interface. This class updates some of
 * the Harvestable's properties excluding the STATUS, the status has to be handled
 * on the higher level.
 * 
 * @author jakub
 */
public class OAIRecordHarvestJob extends AbstractRecordHarvestJob 
{
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private OaiPmhResource resource;
    private Proxy proxy;
    private final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private String currentDateFormat;
    private boolean initialRun = true;
	private TransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
	private Templates[] templates; 

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
        //this.resource.setMessage(null);
    }
    
    @Override
    public void run() {
        setStatus(HarvestStatus.RUNNING);
        this.resource.setMessage(null);
        //figure out harvesting period, eventhough we may end up using
        //resumptionTokens from the DB
        Date nextFrom = null;
        String[] template_filenames = { "oai_dc.xsl", ""};
        
        try {
			templates = getTemplates(template_filenames);
		} catch (TransformerConfigurationException e1) {
            setStatus(HarvestStatus.ERROR);
            resource.setMessage(e1.getMessage());
            logger.log(Level.ERROR, "Error creating normalization transformation.");
            return ;
		} 
        
        if (resource.getUntilDate() != null)
            logger.log(Level.INFO, "JOB#"+resource.getId()+
                    " OAI harvest: until param will be overwritten to yesterday.");
        resource.setUntilDate(yesterday());
        nextFrom = new Date();        
        logger.log(Level.INFO, "JOB#"+resource.getId()+ " OAI harvest started. Harvesting from: "
                + resource.getFromDate() + " until: " + resource.getUntilDate());        
        try {
            
            harvest(resource.getUrl(), 
	                formatDate(resource.getFromDate()), 
	                formatDate(resource.getUntilDate()), 
	                resource.getMetadataPrefix(), 
	                resource.getOaiSetName(),
	                resource.getNormalizationFilter(),
	                getStorage());
            //}
            
        } catch (IOException e) {
            setStatus(HarvestStatus.ERROR);
            resource.setMessage(e.getMessage());
            logger.log(Level.DEBUG, e);
        }
        // if there was no error we move the time marker
        if (getStatus() != HarvestStatus.ERROR && getStatus() != HarvestStatus.KILLED) {
            //TODO persist until and from, trash resumption token
            resource.setFromDate(nextFrom);
            resource.setUntilDate(null);
            resource.setNormalizationFilter(null);
            setStatus(HarvestStatus.FINISHED);
            logger.log(Level.INFO, "JOB#"+resource.getId()+
                    " OAI harvest finished OK. Next from: "
                    + resource.getFromDate());
            initialRun = false;
            try {
                getStorage().commit();
            } catch (IOException ioe) {
                setStatus(HarvestStatus.ERROR);
                resource.setMessage(ioe.getMessage());
                logger.log(Level.ERROR, "Storage commit failed.");
            }
        } else {
            if (getStatus().equals(HarvestStatus.KILLED)) setStatus(HarvestStatus.FINISHED);
            logger.log(Level.INFO, "JOB#"+resource.getId()+" OAI harvest killed/faced error " +
                    "- rolling back. Next from param: " + resource.getFromDate());
            try {
                getStorage().rollback();
            } catch (IOException ioe) {
                logger.log(Level.ERROR, "Storage rollback failed.");
            }            
        }
    }

    private Templates[] getTemplates(String[] filenames) throws TransformerConfigurationException {
    	
    	Templates[] templates = new Templates[filenames.length];
    	int index = 0;
    	for (String filename : filenames) {
    		templates[index] = stf.newTemplates(new StreamSource(new File(filename)));
    		index++;
    	}
		return templates;
	}

	private void harvest(String baseURL, String from, String until,
            String metadataPrefix, String setSpec, String resumptionToken,
            RecordStorage storage) throws IOException {

        Map<String, String> map = new HashMap<String, String>();
        map.put("from", from);
        map.put("until", until);
        map.put("metadataprefix", metadataPrefix);
        map.put("oaisetname", setSpec);
        map.put("normalizationfilter", resource.getNormalizationFilter());

    	ListRecords listRecords = null;
        //resumption Token present in DB?
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
                // The error message has been logged, but print out the full record
                logger.log(Level.DEBUG, "JOB#"+resource.getId()+" OAI error response: \n"
                        + listRecords.toString());
                // if this is noRecordsMatch and initial run, something is wrong
                if (errors.length == 1 &&
                    errors[0].getCode().equalsIgnoreCase("noRecordsMatch") &&
                    !this.initialRun) {
                    logger.log(Level.INFO, "JOB#"+resource.getId()+
                            " noRecordsMatch experienced for non-initial harvest - ignoring");
                    setStatus(HarvestStatus.KILLED);
                    return;
                } else throw new IOException("OAI error "
                        + errors[0].code + ": " + errors[0].message);
            } else {
                if (!dataStart) {
                    storage.databaseStart(map);
                    dataStart = true;
                }
                NodeList list;
				try {
					list = listRecords.getNodeList("/");
					for (int index = 0; index < list.getLength(); index++) {
						Node node = list.item(index);
						Record record = createRecord(node);
						// TODO the createRecord add the record to the storage.
						// Alway null
						if (record != null) {
							if (isDelete(record))
								storage.delete(record.getId());
							else
								storage.add(record);
						}
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
                break;
            } else {
                logger.log(Level.INFO, "Records stored, next resumptionToken is " + resumptionToken);
                resource.setNormalizationFilter(resumptionToken);
                markForUpdate();
                listRecords = listRecords(baseURL, resumptionToken);
            }            
        }
        if (dataStart)
            getStorage().databaseEnd();
    }

	private boolean isDelete(Record node) {
		// TODO Implement
		return false;
	}

	protected Record createRecord(Node node) throws TransformerException {

		DOMSource xmlSource = new DOMSource(node); 
		
		SAXResult outputTarget = new SAXResult(new Pz2SolrRecordContentHandler(getStorage(), resource.getId().toString()));
		Transformer transformer; 
		for (Templates template : templates ) {
			transformer = template.newTransformer();
			DOMResult result = new DOMResult();
			transformer.transform(xmlSource, result);
			xmlSource = new DOMSource(result.getNode());
		}
		transformer = stf.newTransformer();
		transformer.transform(xmlSource, outputTarget);
		return null;
	}

	private ListRecords listRecords(String baseURL, String from, String until, String setSpec, String metadataPrefix) throws IOException {
        try {
            return new ListRecords(baseURL, from, until, setSpec, metadataPrefix, proxy);
        } catch (ResponseParsingException hve) {
            String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
            logger.log(Level.DEBUG, "JOB#" + resource.getId() + msg + " Erroneous respponse:\n" + TextUtils.readStream(hve.getResponseStream()));
            throw new IOException(msg, hve);
        } catch (IOException io) {
            throw io;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private ListRecords listRecords(String baseURL, String resumptionToken) throws IOException {
        try {
            return new ListRecords(baseURL, resumptionToken, proxy);
        } catch (ResponseParsingException hve) {
            String msg = "ListRecords (" + hve.getRequestURL() + ") failed. " + hve.getMessage();
            logger.log(Level.ERROR, msg + " Erroneous respponse:\n" + TextUtils.readStream(hve.getResponseStream()));
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

	@Override
	public void setStorage(HarvestStorage storage) {
		if (!(storage instanceof RecordStorage))
			throw new RuntimeException("Requires a RecordStorage");
		setStorage((RecordStorage) storage);
	}
}