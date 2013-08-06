package com.indexdata.masterkey.localindices.entity;

public class FileStorageEntity extends Storage {

  /**
   * 
   */
  private static final long serialVersionUID = -4384501844217904421L;

  public String getSearchUrl(Harvestable resource) {
    if (resource == null)
      	return getUrl();
    StringBuffer clientUrl = new  StringBuffer(getUrl());
    if (clientUrl.lastIndexOf("/") + 1 != clientUrl.length())
      clientUrl.append('/');
    clientUrl.append(resource.getId());
    return clientUrl.toString();
  }

}
