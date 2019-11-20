package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListSets;

/**
 * Controller of OAI-PMH lookups (sets, meta-data formats, identify)
 * 
 * @author Niels Erik
 *
 */

public class OaiPmhLookups {
  private static Logger logger = Logger.getLogger(OaiPmhLookups.class);

  public com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify getIdentify (String oaiRepositoryUrl)
  throws OaiPmhResourceException {
    logger.debug("Fetching Identify from " + oaiRepositoryUrl);
    com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify identify = null;
    OaiPmhResponse response = null;
    Identify identifyVerb = null; 
    try {
      identifyVerb = new Identify(oaiRepositoryUrl,null,null,logger);
    } catch (ResponseParsingException e) {   
      throw new OaiPmhResourceException("Could not parse result (identify) from " + oaiRepositoryUrl,e);
    } catch (IOException e) {
      throw new OaiPmhResourceException("Could not fetch OAI-PMH response (identify) from " + oaiRepositoryUrl,e);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }      
    response = (OaiPmhResponse) OaiPmhResponseParser.getParser().getDataObject(identifyVerb);
    if (response != null) {
      // Allow for either "Identify" or "identify"
      if (response.getOneElement("Identify") != null) {
        identify = (com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify) response.getOneElement("Identify");
      } else {
        identify = (com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify) response.getOneElement("identify");
      }
    }
    if (identify == null) {
      throw new OaiPmhResourceException("No \"Identify\" element in OAI-PMH response from " + oaiRepositoryUrl);
    }
    return identify;
  }
  
  public List<Set> getSets(String oaiRepositoryUrl) throws OaiPmhResourceException {
    ListSets listSets = null;
    Sets sets = null;
    logger.debug("Fetching sets from " + oaiRepositoryUrl);
    try {
      listSets = new ListSets(oaiRepositoryUrl,null,null,logger);
      sets = (Sets) OaiPmhResponseParser.getParser().getDataObject(listSets).getOneElement("ListSets");
    } catch (ResponseParsingException e) {
      throw new OaiPmhResourceException("Could not parse result (sets) from " + oaiRepositoryUrl,e);
    } catch (IOException e) {
      throw new OaiPmhResourceException("Could not fetch OAI-PMH response (sets) from " + oaiRepositoryUrl,e);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    if (sets != null) {
      return sets.getSets();
    } else {
      throw new OaiPmhResourceException("No sets found at " + oaiRepositoryUrl);
    }
  }
  
  
  public List<MetadataFormat> getMetadataFormats(String oaiRepositoryUrl)  throws OaiPmhResourceException {
    MetadataFormats metadataFormats = null;
    logger.debug("Fetching metadata formats from " + oaiRepositoryUrl);
    ListMetadataFormats listMetadataFormats = null; 
    try {
      listMetadataFormats = new ListMetadataFormats(oaiRepositoryUrl,null,null,logger);
    } catch (ResponseParsingException e) {   
      throw new OaiPmhResourceException("Could not parse result (metadata formats) from " + oaiRepositoryUrl,e);
    } catch (IOException e) {
      throw new OaiPmhResourceException("Could not fetch OAI-PMH response (metadata formats) from " + oaiRepositoryUrl,e);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }      
    metadataFormats = (MetadataFormats) OaiPmhResponseParser.getParser().getDataObject(listMetadataFormats).getOneElement("ListMetadataFormats");      
    if (metadataFormats != null) {
      return metadataFormats.getMetadataFormats();
    } else {
      throw new OaiPmhResourceException("Could not fetch metadata formats from " + oaiRepositoryUrl);
    }
  }
  
}
