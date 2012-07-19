/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAOFactory;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.SplitStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

/**
 * The controller for the Admin interface for Transformations, implements all
 * the business logic and controls data access through DAO object
 * 
 * @author Dennis
 */
public class StepController {
  private Logger logger = Logger.getLogger(getClass());
  // Transformation
  private TransformationStepDAO dao;
  private TransformationStepAssociationDAO associationDao;
  private TransformationStep current;
  private String splitAt;
  private String splitSize;
  
  private DataModel model;
  @SuppressWarnings("rawtypes")
  /* Transformations */
  private List resources;
  /* Steps for current transformations */
  // private List<TransformationStepAssociation> stepAssociation = null;
  private String stepMode = "hideEditStep();"; // which JS function should be
					       // called on load
  Stack<String> backActions = new Stack<String>();
  String homeAction = "home";

  public StepController() {
    try {
      dao = TransformationStepDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
      associationDao = TransformationStepAssociationDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
/*
      stepDao = TransformationStepDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
*/
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }

  public void setTransformation(TransformationStep resource) {
    this.current = resource;
    
  }

  // </editor-fold>
  // <editor-fold defaultstate="collapsed"
  // desc="Transformation list paging functions">
  private int firstItem = 0;
  private int batchSize = 50;
  private int itemCount = -1;

  public int getBatchSize() {
    return batchSize;
  }

  public int getFirstItem() {
    return firstItem;
  }

  public int getLastItem() {
    int count = getItemCount();
    return (count < firstItem + batchSize) ? count : firstItem + batchSize;
  }

  public int getItemCount() {
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (itemCount == -1 || !isPb() && req.getAttribute("countRequestSeenFlag") == null) {
      req.setAttribute("countRequestSeenFlag", "yes");
      itemCount = dao.getCount();
    }
    return itemCount;
  }

  public String next() {
    if (firstItem + batchSize < getItemCount()) {
      firstItem += batchSize;
    }
    return list();
  }

  public String prev() {
    firstItem -= batchSize;
    if (firstItem < 0) {
      firstItem = 0;
    }
    return list();
  }

  public String list() {
    current = null;
    resources = null;
    itemCount = -1;
    return "list_steps";
  }

  public String prepareSplitStep() {
    // TODO create SplitStep
    current = new SplitStep();
    return "edit_split_step";
  }

  public String prepareXslStep() {
    current = new BasicTransformationStep();
    return "edit_xsl_step";
  }

  public String add() {
    prePersist();
    dao.create(current);
    current = null;
    return list();
  }

  /* update resource */
  public String prepareToEdit() {
    current = getResourceFromRequestParam();
    // stepAssociation = current.getStepAssociations();
    postDePersist();
    logger.log(Level.INFO, "Retrieved persisted resource of type " + current.getClass().getName());
    if (current instanceof BasicTransformationStep) {
      return "edit_xsl_step";
    }
    if (current instanceof TransformationStep) {
      return "edit_split_step";
    }
    else {
      logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
      return "failure";
    }
  }

  public String save() {
    prePersist();
    current = dao.update(current);
    current = null;
    return list();
  }

  private boolean isPb() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
  }

  /* list resources */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public DataModel getSteps() {
    // check if new request
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (resources == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
      req.setAttribute("listRequestSeen", "yes");
      resources = (List) dao.retrieveBriefs(firstItem, batchSize);
    }
    if (resources != null)
      Collections.sort(resources);
    return new ListDataModel(resources);
  }

  public String delete() {
    current = getResourceFromRequestParam();
    if (current != null) {
      dao.delete(current);
      // TODO return some error message 
    }
    current = null;
    return list();
  }

  private void prePersist() {

  }

  private void postDePersist() {

  }

  // </editor-fold>

  /* objects from request */
  public TransformationStep getResourceFromRequestParam() {
    TransformationStep o = null;
    if (model != null) {
      o = (TransformationStep) model.getRowData();
      // o = em.merge(o);
    } else {
      String param = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get("id");
      Long id = new Long(param);
      o = dao.retrieveById(id);
    }
    return o;
  }

  public String stackBackAction(String newAction) {
    return backActions.push(newAction);
  }

  public String back() {
    if (backActions.isEmpty())
      return homeAction;
    return backActions.pop();
  }

  public String home() {
    return homeAction;
  }

  public String getHomeAction() {
    return homeAction;
  }

  public void setHomeAction(String homeAction) {
    this.homeAction = homeAction;
  }

  public String addXslStep() {
    // Step up Xsl Step type and association
    logger.error("Setting up new XSL step.");
    BasicTransformationStep step = new BasicTransformationStep();
    step.setDescription("<Description>");
    step.setScript("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    stepMode = "showEditStep();";
    return "new_xsl_step";
  }

  public String editStep() {
    stepMode = "showEditStep();";
    // TODO edit depending on Step Type
    return "edit_xsl_step";
  }

  public String deleteStep() {
    stepMode = "hideEditStep();";
    prePersist();
    if (current != null) {
      if (associationDao.getTransformationCountByStepId(current.getId()) > 0)
	return "step_in_use";
      dao.delete(current);
      stepMode = "hideEditStep();";
      return "delete_step";
    }
    return "no_step";
  }

  public String cancel() {
    return list();
  }

  public TransformationStep getTransformationStep() {
    if (current == null) {
      TransformationStep tmpStep = new BasicTransformationStep("", "", "");
      return tmpStep;
    }
    return current;
  }

  @SuppressWarnings("unchecked")
  public List<SelectItem> getTransformationItems() {
    List<SelectItem> list = new LinkedList<SelectItem>();
    if (resources == null) {
      /* TODO We need all (enabled) storages. Not just a window */
      getSteps();
    }
    list.add(new SelectItem("", "<Select Transformation>"));
    if (resources != null)
      for (TransformationBrief transformation : (List<TransformationBrief>) resources) {
	if (transformation.isEnabled()) {
	  SelectItem selectItem = new SelectItem();
	  selectItem.setLabel(transformation.getName());
	  selectItem.setValue(transformation.getId());
	  list.add(selectItem);
	}
      }
    return list;
  }

  public String getStepMode() {
    return stepMode;
  }

  public void setStepMode(String stepMode) {
    this.stepMode = stepMode;
  }

  public TransformationStep getCurrent() {
    return current;
  }

  public void setCurrent(TransformationStep current) {
    this.current = current;
  }

  public String getSplitAt() {
    return splitAt;
  }

  public void setSplitAt(String splitAt) {
    this.splitAt = splitAt;
  }

  public String getSplitSize() {
    return splitSize;
  }

  public void setSplitSize(String splitSize) {
    this.splitSize = splitSize;
  }

}