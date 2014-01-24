/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

import org.apache.log4j.Logger;

/**
 *
 * @author jakub
 */
public class ConnectorItemSelectedListener implements ValueChangeListener {
  @SuppressWarnings("unused")
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  
  
  @Override
  public void processValueChange(ValueChangeEvent vce) throws
    AbortProcessingException {
    ConnectorItem ci = (ConnectorItem) vce.getNewValue();
    //injection does not work for some reason
    ResourceController rc = (ResourceController) FacesContext.getCurrentInstance().
        getExternalContext().getSessionMap().get("resourceController");
    HarvestConnectorResource hcr = (HarvestConnectorResource) rc.getResource();
    hcr.setName(ci.getDisplayName());
    hcr.setContactNotes(ci.getAuthor());
    hcr.setTechnicalNotes(ci.getNote());
  }
  
}
