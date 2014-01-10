/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.io.CharArrayReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAOFactory;
import com.indexdata.masterkey.localindices.entity.CustomTransformationStep;
import com.indexdata.masterkey.localindices.entity.SplitStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;
import com.indexdata.xml.factory.XmlFactory;

/**
 * The controller for the Admin interface for Transformations, implements all
 * the business logic and controls data access through DAO object
 * 
 * @author Dennis
 */

@ManagedBean(name="stepController")
@SessionScoped
public class StepController {
  private Logger logger = Logger.getLogger(getClass());
  SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  // Transformation
  private TransformationStepDAO dao;
  private TransformationStepAssociationDAO associationDao;
  private TransformationStep current;
  private String splitAt;
  private String splitSize;
  // </editor-fold>
  // <editor-fold defaultstate="collapsed"
  // desc="Transformation list paging functions">
  private int firstItem = 0;
  private int batchSize = 20;
  private int itemCount = -1;

  private boolean selectStepMode = false;

  @SuppressWarnings("rawtypes")
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
  
  private String mode;

  public String getMode() {
    return mode;
  }

  public StepController() {
    try {
      dao = TransformationStepDAOFactory.getDAO((ServletContext) FacesContext.getCurrentInstance()
	  .getExternalContext().getContext());
      associationDao = TransformationStepAssociationDAOFactory.getDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
      /*
       * stepDao = TransformationStepDAOFactory.getDAO((ServletContext)
       * FacesContext .getCurrentInstance().getExternalContext().getContext());
       */
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }

  public void setTransformation(TransformationStep resource) {
    this.current = resource;

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
    logger.debug("stepController#list called");
    current = null;
    resources = null;
    itemCount = -1;
    if (isSelectStepMode())
      return "insert_step";
    return "list_steps";
  }

  public String selectSteps() {
    setSelectStepMode(true);
    return list();
  }

  public String listSteps() {
    setSelectStepMode(false);
    return list();
  }

  
  public String prepareStep() {
    String className = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("entityClass");
    if (className == null) {
      logger.log(Level.INFO, "prepareStep: defaulting to entity class CustomTransformationStep");
      className = "CustomTransformationStep";
    }
    else 
      logger.log(Level.INFO, "prepareStep: entity class " + className);
    return prepareStepCustom(className, null);
  }

  @SuppressWarnings("rawtypes")
  public String prepareStepCustom(String name, String customClass) {
    try {
      Class stepClass = Class.forName("com.indexdata.masterkey.localindices.entity." +  name);
      current = (TransformationStep) stepClass.newInstance();
      if (customClass == null) 
	customClass = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("customClass");
      if (customClass != null) {
	logger.log(Level.INFO, "prepareStep: customClass: " + customClass);
	current.setCustomClass(customClass);
      }
      else
	logger.log(Level.WARN, "prepareStep: Missing customClass");
      //current = new CustomTransformationStep();
      logger.log(Level.WARN, "JSF event " + "edit_" + name);
      return "edit_" + name;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return "error";
    } catch (InstantiationException e) {
      e.printStackTrace();
      return "error";
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return "error";
    }
  }

  public String prepareSplitStep() {
    // TODO create SplitStep
    current = new SplitStep();
    mode = "add";
    return "edit_split_step";
  }

  public String prepareCustomStep() {
    return prepareStepCustom("CustomTransformationStep", null);
  }

  public String prepareXsltStep() {
    current = new XmlTransformationStep();
    mode = "add";
    return "edit_xslt_step";
  }

  public String prepareXmlLogStep() {
    current = new CustomTransformationStep();
    current.setCustomClass("com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter");
    mode = "add";
    return "edit_CustomTransformationStep";
  }

  public String prepareValidationStep() {
    current = new XmlTransformationStep();
    mode = "add";
    return "edit_xsd_step";
  }

  public String add() {
    prePersist();
    dao.create(current);
    current = null;
    return list();
  }

  public String check() {
    prePersist();
    StreamSource template = new StreamSource(new CharArrayReader(current.getScript().toCharArray()));
    try {
      Templates templates =  stf.newTemplates(template); 
      @SuppressWarnings("unused")
      Transformer transformer = templates.newTransformer();
    } catch (Exception e) {
      FacesContext.getCurrentInstance().addMessage("stepForm:script", new FacesMessage(e.getMessage(), e.getCause().getMessage()));
      e.printStackTrace();
    }
    dao.update(current);
    postDePersist();
    return "edit_xslt_step";
  }

  public String test() {
    prePersist();
    StreamSource template = new StreamSource(new CharArrayReader(current.getScript().toCharArray()));
    StringWriter writer = new StringWriter();
    try {
      Templates templates =  stf.newTemplates(template); 
      Transformer transformer = templates.newTransformer();
      transformer.transform(new StreamSource(new CharArrayReader(current.getTestData().toCharArray())), new StreamResult(writer));
      current.setTestOutput(writer.toString());
    } catch (Exception e) {
      FacesContext.getCurrentInstance().addMessage("stepForm:testData", new FacesMessage(e.getMessage(), e.getCause().getMessage()));
      //current.setTestOutput("Failed to run test");
      e.printStackTrace();
    }
    dao.update(current);
    return "edit_xslt_step";
  }

  /* update resource */
  public String prepareToEdit() {
    mode = "edit";
    current = getResourceFromRequestParam();
    postDePersist();
    String className = current.getClass().getSimpleName();
    logger.log(Level.INFO, "Retrieved persisted resource of type " + className);
    if (current instanceof XmlTransformationStep) {
      return "edit_xslt_step";
    }
    if (current instanceof CustomTransformationStep) {
      return "edit_CustomTransformationStep";
    } else {
      logger.log(Level.INFO, "Unknown resource type. No matching form defined. Using class name: " + className);
      return "edit_"  + className;
    }
  }

  public String save() {
    prePersist();
    if (current.getId() != null)
      current = dao.update(current);
    else
      dao.create(current);
    current = null;
    return list();
  }

  private boolean isPb() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
  }

  /* list resources */
  @SuppressWarnings("rawtypes")
  public DataModel getSteps() {
    setSelectStepMode(false);
    return getStepsCommon();
  }

  /* list resources */
  @SuppressWarnings("rawtypes")
  public DataModel getStepsInsertMode() {
    setSelectStepMode(true);
    return getStepsCommon();
  }

  /* list resources */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public DataModel getStepsCommon() {
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
      try {
        dao.delete(current);
      } catch (EntityInUse eiu) {
        logger.warn("Cannot remove step", eiu);
      }
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
    XmlTransformationStep step = new XmlTransformationStep();
    step.setDescription("<Description>");
    step.setScript("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    stepMode = "showEditStep();";
    return "new_xsl_step";
  }

  public String editStep() {
    // TODO edit depending on Step Type
    return "edit_xslt_step";
  }

  public String deleteStep() {
    prePersist();
    if (current != null) {
      if (associationDao.getTransformationCountByStepId(current.getId()) > 0)
	return "step_in_use";
      try {
        dao.delete(current);
      } catch (EntityInUse eiu) {
        //never gets here
      }
      return list();
    }
    return "failure";
  }

  public String cancel() {
    return list();
  }

  public TransformationStep getTransformationStep() {
    if (current == null) {
      TransformationStep tmpStep = new XmlTransformationStep("", "", "");
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

  public boolean isSelectStepMode() {
    return selectStepMode;
  }

  public void setSelectStepMode(boolean selectStepMode) {
    this.selectStepMode = selectStepMode;
  }

  public List<SelectItem> getStepClasses() {
    List<SelectItem> list = new LinkedList<SelectItem>();
    Reflections reflections = new Reflections("com.indexdata");
    Set<Class<? extends TransformationStep>> subTypes = reflections.getSubTypesOf(TransformationStep.class);
    for (Class<? extends TransformationStep> step : subTypes) {
      SelectItem item = new SelectItem(); 
      item.setLabel(step.getSimpleName());
      item.setValue(step.getName());
    }
    return list;
  }
  

}
