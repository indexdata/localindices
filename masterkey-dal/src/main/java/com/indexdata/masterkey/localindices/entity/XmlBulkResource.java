/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author jakub
 */
@Entity
@XmlRootElement(name = "xmlBulk")
public class XmlBulkResource extends Harvestable implements Serializable {

  private static final long serialVersionUID = -6751028242629873367L;
  @Column(length = 16384)
  private String url;
  private String splitAt;
  private String splitSize;
  private String expectedSchema;
  private String outputSchema;
  @Column(nullable=false)
  private boolean allowCondReq;

  public XmlBulkResource() {
  }

  public XmlBulkResource(String url) {
    this.url = url;
  }

  public String getExpectedSchema() {
    return expectedSchema;
  }

  public void setExpectedSchema(String expectedSchema) {
    this.expectedSchema = expectedSchema;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not
    // set
    if (!(object instanceof XmlBulkResource)) {
      return false;
    }
    Harvestable other = (Harvestable) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  public void setSplitAt(String splitAt) {
    this.splitAt = splitAt;
  }

  public String getSplitAt() {
    return splitAt;
  }

  public void setSplitSize(String splitSize) {
    this.splitSize = splitSize;
  }

  public String getSplitSize() {
    return splitSize;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public boolean getAllowCondReq() {
    return allowCondReq;
  }

  public void setAllowCondReq(boolean allowCondReq) {
    this.allowCondReq = allowCondReq;
  }
  
}
