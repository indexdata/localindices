package com.indexdata.masterkey.localindices.entity;

public class HarvestConnectorResource extends Harvestable {
  
  /**
   * 
   */
  private static final long serialVersionUID = 4240798904624525089L;
  private String initData;  	
  private String connector;
  private String startData;
  private String endDate;
  private String resumptionToken;
  private String isPersistence;
  private String url;
  
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

}
