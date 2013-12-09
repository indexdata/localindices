package com.indexdata.masterkey.localindices.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.utils.TextUtils;

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
  @Column(length = 4096,name = "CONNECTORURL")
  private String connector;
  @ManyToOne(optional = true)
  private Setting connectorEngineUrlSetting;
  @ManyToOne(optional = true)
  private Setting connectorRepoUrlSetting;
  @Temporal(TemporalType.TIMESTAMP)
  private Date fromDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date untilDate;
  private String resumptionToken;
  private String isPersistence;
  private Long sleep;
  private String engineParameters;
  
  public String getConnectorUrl() {
    if (connectorRepoUrlSetting == null) return null;
    return TextUtils.joinPath(connectorRepoUrlSetting.getValue(), getConnector());
  }
  
  public String getUrl() {
    return connectorEngineUrlSetting == null ? null : connectorEngineUrlSetting.getValue();
  }

  public Setting getConnectorEngineUrlSetting() {
    return connectorEngineUrlSetting;
  }

  public void setConnectorEngineUrlSetting(Setting connectorEngineUrlSetting) {
    this.connectorEngineUrlSetting = connectorEngineUrlSetting;
  }

  public Setting getConnectorRepoUrlSetting() {
    return connectorRepoUrlSetting;
  }

  public void setConnectorRepoUrlSetting(Setting connectorRepoUrlSetting) {
    this.connectorRepoUrlSetting = connectorRepoUrlSetting;
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

  public String getConnector() {
    //in casee the input is full url we split it
    return connector == null ? null : connector.replaceFirst(".*[/]", "");
  }

  public void setConnector(String connector) {
    this.connector = connector;
  }

  public void reset() {
    super.reset();
    setFromDate(null);
    setUntilDate(null);
    setResumptionToken(null);
  }

  public String getEngineParameters() {
    return engineParameters;
  }

  public void setEngineParameters(String engineParameters) {
    this.engineParameters = engineParameters;
  }

}
