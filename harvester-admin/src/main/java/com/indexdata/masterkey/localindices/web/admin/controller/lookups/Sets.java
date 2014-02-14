package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Sets extends OaiPmhResponse {

  private static final long serialVersionUID = 2461406533997740049L;
  private final static Logger logger = Logger.getLogger(OaiPmhResponse.class);
  List<Set> sets = new ArrayList<Set>();
  
  public List<Set> getSets () {
    if (sets.size()==0) {
      if (getElements("set") != null) {
        for (ResponseDataObject element : getElements("set")) {
          sets.add((Set)element);
        }
      }
      logger.debug("Had zero elements, added " + sets.size() + " elements.");
    }
    return sets;
  }
}
