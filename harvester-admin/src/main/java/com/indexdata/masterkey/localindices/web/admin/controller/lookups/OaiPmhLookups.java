package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.io.IOException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListSets;

/**
 * Controller of lookups (sets, meta-data formats, identify)
 * 
 * @author Niels Erik
 *
 */

@ManagedBean(name = "oaiPmhLookups")
@ViewScoped
public class OaiPmhLookups {
  private static Logger logger = Logger.getLogger(OaiPmhLookups.class);
  private String oaiRepositoryUrl = "";  
  private String metadataFormat = "";
  private String setName = "";
  private String setName2 = "";
  private String setNameFilter = "";
  Sets sets = new Sets();
  MetadataFormats metadataFormats = new MetadataFormats();
  com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify identify 
     = new com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify();

  public boolean getRender() {
    return oaiRepositoryUrl.length()>0;
  }
  
  public void setSetNameFilter (String string) {
    setNameFilter = string;
  }
  
  public String getSetNameFilter () {
    return setNameFilter;
  }
      
  public void setMetadataFormat (String prefix) {
    metadataFormat = prefix;
  }
  
  public String getMetadataFormat () {
    return metadataFormat;
  }
  
  public void setSetName (String setName) {
    logger.debug("Setting set to " + setName);
    this.setName = setName;
  }
  
  public String getSetName () {
    return setName;
  }
  
  public void setSetName2 (String setName) {
    this.setName2 = setName;
  }
  
  public String getSetName2 () {
    return setName2;
  }

  public void setOaiRepositoryUrl (String oaiRepositoryUrl) {
    logger.debug("Setting OAI URL to " + oaiRepositoryUrl);
    this.oaiRepositoryUrl = oaiRepositoryUrl;
    this.setNameFilter = "";
    this.sets = new Sets();
    this.metadataFormats = new MetadataFormats();
    this.identify = new com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify();
  }
  
  public String getOaiRepositoryUrl () {
    return oaiRepositoryUrl;
  }
  
  public com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify getIdentify () {
    logger.debug("Request to get Identify");
    if (oaiRepositoryUrl.length()>0 && identify.isEmpty()) {
      fetchIdentify();
    }
    return identify;
  }
  
  private void fetchIdentify () {
    logger.debug("Fetching Identify from " + oaiRepositoryUrl);
    OaiPmhResponse response = null;
    Identify identifyVerb = null; 
    try {
      identifyVerb = new Identify(oaiRepositoryUrl,null,null,logger);
    } catch (ResponseParsingException e) {   
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }      
    response = (OaiPmhResponse) OaiPmhResponseParser.getParser().getDataObject(identifyVerb);
    if (response != null && response.getOneElement("Identify") != null) { 
      identify = (com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify) response.getOneElement("Identify");
    }
  }
    
  public List<Set> getSets() {
    logger.debug("Request to get sets from [" + oaiRepositoryUrl + "]");      
    if (oaiRepositoryUrl.length()>0 && sets.getSets().size()==0) {
      fetchSets();
    }
    return sets.getSets();
  }
  
  private void fetchSets () {    
    ListSets listSets = null;     
    if (oaiRepositoryUrl.length()>0) {
      logger.debug("Fetching sets from " + oaiRepositoryUrl);
      try {
        listSets = new ListSets(oaiRepositoryUrl,null,null,logger);
        sets = (Sets) OaiPmhResponseParser.getParser().getDataObject(listSets).getOneElement("ListSets");
      } catch (ResponseParsingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (TransformerException e) {
        e.printStackTrace();
      }
    }
  }
  
  public List<MetadataFormat> getMetadataFormats() {
    logger.debug("Request to get metadataFormats");        
    if (oaiRepositoryUrl.length()>0 && metadataFormats.count()==0) {
      fetchMetadataFormats();
    }      
    return metadataFormats.getMetadataFormats();  
  }
  
  private void fetchMetadataFormats () {
    if (oaiRepositoryUrl.length()>0) {
      logger.debug("Fetching metadata formats from " + oaiRepositoryUrl);
      ListMetadataFormats listMetadataFormats = null; 
      try {
        listMetadataFormats = new ListMetadataFormats(oaiRepositoryUrl,null,null,logger);
      } catch (ResponseParsingException e) {   
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (TransformerException e) {
        e.printStackTrace();
      }      
      metadataFormats = (MetadataFormats) OaiPmhResponseParser.getParser().getDataObject(listMetadataFormats).getOneElement("ListMetadataFormats");      
    }
  }
  
}
