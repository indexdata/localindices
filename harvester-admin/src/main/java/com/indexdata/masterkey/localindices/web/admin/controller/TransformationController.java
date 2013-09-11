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
import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAOFactory;
import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

/**
 * The controller for the Admin interface for Transformations, implements all
 * the business logic and controls data access through DAO object
 * 
 * @author Dennis
 */
public class TransformationController {
  private Logger logger = Logger.getLogger(getClass());
  // Transformation
  private TransformationDAO dao;
  private TransformationStepAssociationDAO associationDao;
  //private TransformationStepDAO stepDao;
  private Transformation current;

  @SuppressWarnings("rawtypes")
  private DataModel model;
  @SuppressWarnings("rawtypes")
  /* Transformations */
  private List resources;
  /* Steps for current transformations */
  // private List<TransformationStepAssociation> stepAssociation = null;
  private String stepMode = "hideEditStep();"; // which JS function should be
					       // called on load
  private TransformationStepAssociation currentStepAssociation;
  private TransformationStepDAO stepDao; 
  
  Stack<String> backActions = new Stack<String>();
  String homeAction = "home";
  // </editor-fold>
  // <editor-fold defaultstate="collapsed"
  // desc="Transformation list paging functions">
  private int firstItem = 0;
  private int batchSize = 20;
  private int itemCount = -1;
  private String errorMessage;


  public TransformationController() {
    try {
      dao = TransformationDAOFactory.getTransformationDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
      associationDao = TransformationStepAssociationDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());

      stepDao = TransformationStepDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());

    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }

  public Transformation getTransformation() {
    return current;
  }

  public void setTransformation(Transformation resource) {
    this.current = resource;
    // currentStepAssociation = current.getStepAssociations();
  }

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
    resources = null;
    itemCount = -1;
    return "list_transformations";
  }

  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="DAO methods">
  /* add new resource */
  public String prepareTransformationToAdd() {
    current = new BasicTransformation();
    return "new_transformation";
  }

  public String prepareCrossMapTransformationToAdd() {
    // TODO
    // current = new CrossMapTransformation();
    return "new_crossmap_transformation";
  }

  /* Save and continue editing */ 
  public String save() {
    prePersist();
    if (current.getId() == null)
      dao.create(current);
    else
      current = dao.update(current);
    return editCurrent();
  }

  public String saveExit() {
    save();
    current = null;
    currentStepAssociation = null;
    return list();
  }

  /* update resource */
  public String prepareToEdit() {
    current = getResourceFromRequestParam();
    // stepAssociation = current.getStepAssociations();
    currentStepAssociation = null;
    postDePersist();
    logger.log(Level.INFO, "Retrieved persisted resource of type " + current.getClass().getName());
    if (current instanceof Transformation) {
      return "edit_transformation";
    }
    /*
     * else if (resource instanceof ZebraTransformation) { return
     * "edit_zebraTransformation"; } else if (resource instanceof
     * ConsoleTransformation) { return "edit_console"; } else if (resource
     * instanceof XmlTransformation) { return "edit_xmlTransformation"; }
     */
    else {
      logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
      return "failure";
    }
  }

  private boolean isPb() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
  }

  /* list resources */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public DataModel getTransformations() {
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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public DataModel getTransformationSteps() {
    List<TransformationStepAssociation> steps = new LinkedList<TransformationStepAssociation>(); 
    
    /*
     * stepDao . retrieveByTransformationId ( current . getId ( ) ) ;
     */
    if (current != null)
      steps = (List) current.getSteps();
    return new ListDataModel(steps);
  }

  public String delete() {
    current = getResourceFromRequestParam();
    dao.delete(current);
    current = null;
    return list();
  }

  public String saveAndPurge() {
    dao.delete(current);
    prePersist();
    current.setId(null);
    dao.create(current);
    current = null;
    return list();
  }

  private void prePersist() {

  }

  private void postDePersist() {

  }

  public Transformation getResourceFromRequestParam() {
    return getResourceFromRequestParam(null);
  }

  /* objects from request */
  public Transformation getResourceFromRequestParam(String id_param) {
    if (id_param == null)
      id_param = "id";
    
    Transformation o = null;
    if (model != null) {
      o = (Transformation) model.getRowData();
      // o = em.merge(o);
    } else {
      String param = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get(id_param);
      Long id = new Long(param);
      o = dao.retrieveById(id);
    }
    return o;
  }

  /* objects from request */
  public TransformationStep getStepFromRequestParam(String id_param) 
  {
    if (id_param == null)
      id_param = "id";
    TransformationStep o; 
    String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(id_param);
    Long id = new Long(param);
    o = stepDao.retrieveById(id);
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

  private int setupStep() {
    int index = -1;
    String idName = "stepID";
    String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
	.get(idName);
    if (param != null && !"null".equals(param)) {
      try {
	Long id = new Long(param);
	index = lookupIndexByID(id);
	if (index >= 0)
	  currentStepAssociation = current.getStepAssociations().get(index);
	else
	  currentStepAssociation = null;
      } catch (Exception e) {
	logger.error("Unable to get Step from parameter '" + idName + "' " + param + ". Error: "
	    + e.getMessage());
      }
    }
    logger.debug("Step from parameter '" + idName + "'=" + param + ": " + currentStepAssociation
	+ " index: " + index);

    return index;
  }

  private int lookupIndexByID(Long id) {
    for (int index = 0; index < current.getStepAssociations().size(); index++) {
      if (current.getStepAssociations().get(index).getStep().getId().equals(id))
	return index;
    }
    return -1;
  }

  public String selectStepToInsert() {
    return "insert_step";
  }

  public String addStep() 
  {
    Transformation transformation = getResourceFromRequestParam("transformationID");
    TransformationStep step = getStepFromRequestParam("stepID");
    if (transformation != null && step != null) {
      currentStepAssociation = new TransformationStepAssociation();
      currentStepAssociation.setStep(step);
      currentStepAssociation.setTransformation(transformation);
      currentStepAssociation.setPosition(transformation.getSteps().size() + 1);
      associationDao.create(currentStepAssociation); 
      logger.debug("Association id " + currentStepAssociation.getId() 
	  + " Transformation ID: " + currentStepAssociation.getTransformation().getId()
	  + " Step ID: " + currentStepAssociation.getStep().getId());  
      transformation.addStepAssociation(currentStepAssociation);
      // Should not happpen, but...
      if (transformation.getId() == null) 
	dao.create(transformation);
      current = dao.update(transformation);
    } 
    else {
      errorMessage = "Failed to attached Step (" + step + ") to Transformation (" + transformation + "): One was not found.";  
      return "transformation_failure";
    }
      
    return "insert_step";
  }

  public String editStep() {
    setupStep();
    stepMode = "showEditStep();";
    // TODO edit depending on Step Type
    return "edit_xsl_step";
  }

  public String editCurrent() {
    return "edit_current";
  }

  public String upStep() {
    setupStep();
    upDownStep(currentStepAssociation, -1);
    stepMode = "hideEditStep();";
    // TODO replace with edit_current
    return "up_step";
  }

  private void upDownStep(TransformationStepAssociation association, int i) {
    int index;
    for (index = 0; index < current.getStepAssociations().size(); index++) {
      if (current.getStepAssociations().get(index).equals(association)) {
	break;
      }
    }
    int newIndex = index + i;
    // If found and newIndex is within bounds
    if (index < current.getStepAssociations().size() && newIndex >= 0
	&& newIndex < current.getStepAssociations().size()) {
      TransformationStepAssociation cur = current.getStepAssociations().get(index);
      TransformationStepAssociation swap = current.getStepAssociations().get(newIndex);
      cur.setPosition(cur.getPosition() + i);
      swap.setPosition(swap.getPosition() - i);
      current.getStepAssociations().set(newIndex, cur);
      current.getStepAssociations().set(index, swap);
      current = dao.update(current);
      associationDao.update(cur);
      associationDao.update(swap);
    }
  }

  public String downStep() {
    setupStep();
    upDownStep(currentStepAssociation, 1);
    stepMode = "hideEditStep();";
    // TODO replace with edit_current
    return "down_step";
  }

  public String deleteStep() {
    // TransformationStepAssociation currentStep = null;
    int index = setupStep();
    if (currentStepAssociation != null && currentStepAssociation.getTransformation() != null) {
      currentStepAssociation = current.getStepAssociations().remove(index);
      try {
        associationDao.delete(currentStepAssociation);
      }  catch (EntityInUse eiu) {
        logger.warn("Cannot remove step association", eiu);
      }
      logger.debug("" + currentStepAssociation + " was removed.");
      currentStepAssociation = null;
      // TODO Need reordering!
    }
    stepMode = "hideEditStep();";
    prePersist();
    current = dao.update(current);

    stepMode = "hideEditStep();";
    return "delete_step";
  }

  public String saveStep() {
    // TODO persist current step and association . Not on list, add
    stepMode = "hideEditStep();";
    if (!current.getStepAssociations().contains(currentStepAssociation)) {
      current.addStepAssociation(currentStepAssociation);
      currentStepAssociation.setTransformation(current);
      prePersist();
      associationDao.create(currentStepAssociation);
    } else {
      prePersist();
      currentStepAssociation.setTransformation(current);
      currentStepAssociation = associationDao.update(currentStepAssociation);
    }
    currentStepAssociation = null;
    return "save_step";
  }

  public String cancel() {
    current = null;
    currentStepAssociation = null;
    return list();
  }

  public TransformationStep getTransformationStep() {
    if (currentStepAssociation == null) {
      TransformationStep tmpStep = new XmlTransformationStep("", "", "");
      return tmpStep;
    }
    return currentStepAssociation.getStep();
  }

  public void setTransformationStep(TransformationStepAssociation stepAssociation) {
    this.currentStepAssociation = stepAssociation;
  }

  @SuppressWarnings("unchecked")
  public List<SelectItem> getTransformationItems() {
    List<SelectItem> list = new LinkedList<SelectItem>();
    if (resources == null) {
      /* TODO We need all (enabled) storages. Not just a window */
      getTransformations();
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

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
