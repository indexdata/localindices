package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

public class DummyXmlBulkResource extends XmlBulkResource {

  /**
   * 
   */
  private static final long serialVersionUID = 7350014678423932105L;

  public DummyXmlBulkResource(String url) { 
    super(url);
    setId(1l);
    setName("Test Harvestable");
    setStorage(new com.indexdata.masterkey.localindices.entity.SolrStorageEntity());
    getStorage().setId(1l);
    getStorage().setName("Test Storage");
  }
  
}