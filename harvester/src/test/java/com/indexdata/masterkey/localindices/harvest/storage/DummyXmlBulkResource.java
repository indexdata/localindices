package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;

public class DummyXmlBulkResource extends XmlBulkResource {

  /**
   * 
   */
  private static final long serialVersionUID = 7350014678423932105L;

  public DummyXmlBulkResource(String url) { 
    super(url);
    setId(1l);
    setName("Test Harvestable");
    setCurrentStatus(HarvestStatus.NEW.toString());
    setStorage(new com.indexdata.masterkey.localindices.entity.SolrStorageEntity());
    getStorage().setId(1l);
    getStorage().setName("Test Storage");
  }
 
  
}