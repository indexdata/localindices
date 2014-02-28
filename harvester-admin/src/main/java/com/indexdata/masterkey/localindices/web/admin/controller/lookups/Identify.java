package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

public class Identify extends OaiPmhResponse {

  private static final long serialVersionUID = 8457330148938106109L;
  
  public String getRepositoryName () {
    return getOneValue("repositoryName");
  }
  
  public String getAdminEmail () {
    return getOneValue("adminEmail");
  }
  
  public String getEarliestDatestamp () {
    return getOneValue("earliestDatestamp");
  }
  
  public String getGranularity () {
    return getOneValue("granularity");
  }
  
  public boolean getUseLongDateFormat () {
    return getGranularity().length()>10;
  }
  
  public void setUseLongDateFormat (boolean var) {
    
  }
  
  public boolean isEmpty() {
    return getRepositoryName().length()==0;
  }
  
  public boolean getIsEmpty() {
    return isEmpty();
  }

}
