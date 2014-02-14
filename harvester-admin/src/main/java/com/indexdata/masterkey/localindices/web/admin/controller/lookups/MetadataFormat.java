package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

public class MetadataFormat extends OaiPmhResponse {

  private static final long serialVersionUID = 6872388045127827035L;

  public String getMetadataPrefix () {
    return getOneValue("metadataPrefix");
  }

}
