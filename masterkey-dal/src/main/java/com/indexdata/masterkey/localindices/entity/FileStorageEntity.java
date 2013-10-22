package com.indexdata.masterkey.localindices.entity;

public class FileStorageEntity extends Storage {

  /**
   * 
   */
  private static final long serialVersionUID = -4384501844217904421L;

  public String getSearchUrl(Harvestable resource) {
    if (resource == null)
      	return super.getSearchUrl();
    StringBuffer clientUrl = new  StringBuffer(super.getSearchUrl());
    if (clientUrl.lastIndexOf("/") + 1 != clientUrl.length())
      clientUrl.append('/');
    clientUrl.append(resource.getId());
    return clientUrl.toString();
  }

  public String getIndexingUrl(Harvestable resource) {
    if (resource == null)
      	return super.getIndexingUrl();
    StringBuffer clientUrl = new  StringBuffer(super.getIndexingUrl());
    if (clientUrl.lastIndexOf("/") + 1 != clientUrl.length())
      clientUrl.append('/');
    clientUrl.append(resource.getId());
    return clientUrl.toString();
  }

}
