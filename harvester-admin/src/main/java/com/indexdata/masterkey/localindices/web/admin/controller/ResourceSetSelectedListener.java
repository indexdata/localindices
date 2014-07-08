package com.indexdata.masterkey.localindices.web.admin.controller;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

import org.apache.log4j.Logger;

@ManagedBean(name = "resourceSetSelectedListener")
@ViewScoped
public class ResourceSetSelectedListener implements ValueChangeListener {
  private final static Logger logger = Logger.getLogger(ResourceSetSelectedListener.class);

  @Override
  public void processValueChange(ValueChangeEvent vce)
      throws AbortProcessingException {
    logger.debug("Value change event " + vce);
  }

}
