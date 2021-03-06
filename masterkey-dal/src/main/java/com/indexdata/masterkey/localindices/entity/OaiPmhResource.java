/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

/**
 * 
 * @author jakub
 */
@Entity
@XmlRootElement(name = "oaiPmh")
public class OaiPmhResource extends Harvestable implements Serializable {

  private static final long serialVersionUID = -3980532198473557541L;
  private static Logger logger = Logger.getLogger(OaiPmhResource.class);
  private String url;
  private String oaiSetName;
  private String metadataPrefix;
  private String schemaURI;
  // resumption token
  private String resumptionToken;
  private String dateFormat;
  @Column(nullable = false)
  protected Boolean clearRtOnError = false;
  @Column(nullable = false)
  protected Boolean keepPartial = true;

  public OaiPmhResource() {
  };

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getUrl() {
    logger.debug("Getting url " + url);
    return url;
  }

  public void setUrl(String url) {
    logger.debug("Setting url " + url);
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

  public Boolean getClearRtOnError() {
    return clearRtOnError;
  }

  public void setClearRtOnError(Boolean clearRtOnError) {
    this.clearRtOnError = clearRtOnError;
  }

  public Boolean getKeepPartial() {
    return keepPartial;
  }

  public void setKeepPartial(Boolean keepPartial) {
    this.keepPartial = keepPartial;
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

  public void reset() {
    super.reset();
    setFromDate(null);
    setUntilDate(null);
    setResumptionToken(null);
  }
}
