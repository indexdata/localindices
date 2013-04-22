package com.indexdata.masterkey.localindices.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "connector")
public class HarvestConnectorResource extends Harvestable {
  
  /**
   * 
   */
  private static final long serialVersionUID = 4240798904624525089L;
  private String username;  	
  private String password;  	
  private String proxy;  	
  @Column(length = 4096)
  private String initData;
  @Column(length = 4096)
  private String connectorUrl;
  
  @Temporal(TemporalType.TIMESTAMP)
  private Date fromDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date untilDate;
  private String resumptionToken;
  private String isPersistence;
  @Column(length = 4096)
  private String url;
  private Long   sleep;
  
  public String getConnectorUrl() {
    return connectorUrl;
  }

  public void setConnectorUrl(String connector) {
    this.connectorUrl = connector;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date date) {
    this.fromDate = date;
  }

  public Date getUntilDate() {
    return untilDate;
  }
  
  public void setUntilDate(Date date) {
    this.untilDate = date;
  }

  
  public String getResumptionToken() {
    return resumptionToken;
  }

  public void setResumptionToken(String resumptionToken) {
    this.resumptionToken = resumptionToken;
  }

  public String getIsPersistence() {
    return isPersistence;
  }

  public void setIsPersistence(String isPersistence) {
    this.isPersistence = isPersistence;
  }

  public String getInitData() {
    return initData;
  }

  public void setInitData(String initData) {
    this.initData = initData;
  }

  public void setUrl(String url) {
   this.url = url;
  } 

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProxy() {
    return proxy;
  }

  public void setProxy(String proxy) {
    this.proxy = proxy;
  }

  public Long getSleep() {
    return sleep;    
  }

  public void setSleep(Long sleep) {
    this.sleep = sleep;
  }


}
