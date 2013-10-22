package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "solrStorage")
public class SolrStorageEntity extends Storage implements Serializable {

  private static final long serialVersionUID = -5840585258242340150L;

  public String getSearchUrl(Harvestable resource) {
    if (resource == null)
      	return getSearchUrl();
    StringBuffer clientUrl = new  StringBuffer(getSearchUrl());
    if (clientUrl.lastIndexOf("/") + 1 != clientUrl.length())
      clientUrl.append('/');
    clientUrl.append("select?q=database:").append(resource.getId());
    return clientUrl.toString();
  }
}
