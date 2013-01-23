/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author jakub
 */
@Entity
@XmlRootElement(name = "oaiPmh")
public class OaiPmhResource extends Harvestable implements Serializable {

  private static final long serialVersionUID = -3980532198473557541L;
  private String url;
  private String oaiSetName;
  private String metadataPrefix;
  private String schemaURI;
  // resumption token
  private String resumptionToken;
  @Temporal(TemporalType.DATE)
  private Date fromDate;
  @Temporal(TemporalType.DATE)
  private Date untilDate;
  private String dateFormat;

  public OaiPmhResource() {
  };

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date fromDate) {
    this.fromDate = fromDate;
  }

  public Date getUntilDate() {
    return untilDate;
  }

  public void setUntilDate(Date untilDate) {
    this.untilDate = untilDate;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public void setMetadataPrefix(String metadataPrefix) {
    this.metadataPrefix = metadataPrefix;
  }

  public String getOaiSetName() {
    return oaiSetName;
  }

  public void setOaiSetName(String oaiSetName) {
    this.oaiSetName = oaiSetName;
  }

  public String getSchemaURI() {
    return schemaURI;
  }

  public void setSchemaURI(String schemaURI) {
    this.schemaURI = schemaURI;
  }

  public String getResumptionToken() {
    return resumptionToken;
  }

  public void setResumptionToken(String resumptionToken) {
    this.resumptionToken = resumptionToken;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not
    // set
    if (!(object instanceof OaiPmhResource)) {
      return false;
    }
    Harvestable other = (Harvestable) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

}
