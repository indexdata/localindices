package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

public class Set extends OaiPmhResponse {

  private static final long serialVersionUID = -3545893445003325966L;
  String setSpec = "";
  String setName = "";
  
  public Set() { }
  
  public void setSetSpec (String setSpec) {
    this.setSpec = setSpec;
  }
  
  public String getSetSpec () {
    return getOneValue("setSpec");
  }
  
  public void setSetName (String setName) {
    this.setName = setName;
  }
  
  public String getSetName () {
    return getOneValue("setName");
  }
  
  public String toString () {
    return getOneValue("setSpec") + ": " + getOneValue("setName");
  }

}
