package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
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
  @Column(length = 1024000)
  private String connector;
  private String startData;
  private String endDate;
  private String resumptionToken;
  private String isPersistence;
  @Column(length = 4096)
  private String url;
  private Long   sleep;
  
  public String getConnector() {
    return connector;
  }

  public void setConnector(String connector) {
    this.connector = connector;
  }

  public String getStartDate() {
    return startData;
  }

  public void setStartDate(String fromData) {
    this.startData = fromData;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
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
