package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "solrStorage")
public class SolrStorage extends Storage implements Serializable {

  private static final long serialVersionUID = -5840585258242340150L;

  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

}
