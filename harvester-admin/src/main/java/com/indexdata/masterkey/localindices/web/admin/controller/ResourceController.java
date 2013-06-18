/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
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
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOException;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOFactory;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAOFactory;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

/**
 * The controller for the Admin interface, implements all the business logic and
 * controls data access through DAO object
 * 
 * @author jakub
 * @author Dennis
 */
public class ResourceController {
  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  private HarvestableDAO dao;
  private Harvestable resource;
  @SuppressWarnings("rawtypes")
  private DataModel model;
  private Boolean longDate;
  private final static String SHORT_DATE_FORMAT = "yyyy-MM-dd";
  private final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
  @SuppressWarnings("rawtypes")
  private List resources;
  private String jobLog;
  Stack<String> backActions = new Stack<String>();
  String homeAction = "home";
  // </editor-fold>
  // <editor-fold defaultstate="collapsed"
  // desc="Resource list paging functions">
  private int firstItem = 0;
  private int batchSize = 20;
  private int itemCount = -1;
  private String sortKey = "name";
  private boolean isAsc = true;
  private Long currentId;


  public Boolean getLongDate() {
    return longDate;
  }

  public void setLongDate(Boolean longDate) {
    this.longDate = longDate;
  }

  public ResourceController() {
    try {
      dao = HarvestableDAOFactory.getHarvestableDAO((ServletContext) FacesContext
	  .getCurrentInstance().getExternalContext().getContext());
    } catch (HarvestableDAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }

  public Harvestable getResource() {
    return resource;
  }

  public void setResource(Harvestable resource) {
    this.resource = resource;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<SelectItem> getMetadataPrefixes() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    Properties prefixes = new Properties(); 
    try {
      prefixes.load(getClass().getResourceAsStream("/prefixes.properties"));
    } catch (IOException e) {
      e.printStackTrace();
      prefixes.put("oai_dc", "OAI_DC");
      prefixes.put("marc21", "MARC21/USMARC");
      prefixes.put("pp2-solr", "PP2");
    }
    for (Object obj: prefixes.keySet()) {
	if (obj instanceof String) {
	  String key = (String) obj;
	  String value = prefixes.getProperty(key);
	  SelectItem selectItem = new SelectItem();
	  selectItem.setLabel(value);
	  selectItem.setValue(key);
	  list.add(selectItem);
	}
	else {
	  logger.warn("Expected String key" + obj.getClass() + " " + obj);
	}
    }
    Collections.sort(list, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
	SelectItem s1 = (SelectItem) o1;
	SelectItem s2 = (SelectItem) o2;
	return ((String) s1.getValue()).compareTo((String) s2.getValue());
      }
    });
    return list;
  }

  // <editor-fold defaultstate="collapsed"
  // desc="Harvest schedule handling functions">
  private enum Month {

    Any, January, February, March, April, May, June, July, August, September, October, November, December
  }

  private enum DayOfTheWeek {
    Any("*"), Monday("1"), Tuesday("2"), Wednesday("3"), Thursday("4"), Friday("5"), Saturday("6"), Sunday(
	"0");
    private String fieldValue;

    DayOfTheWeek(String fieldValue) {
      this.fieldValue = fieldValue;
    }

    public String fieldValue() {
      return fieldValue;
    }

  }

  private String min;
  private String hour;
  private String dayOfMonth;
  private String month;
  private String dayOfWeek;

  public String getHour() {
    return hour;
  }

  public void setHour(String hour) {
    this.hour = hour;
  }

  public String getMin() {
    return min;
  }

  public void setMin(String min) {
    this.min = min;
  }

  public String getDayOfMonth() {
    return dayOfMonth;
  }

  public void setDayOfMonth(String dayOfMonth) {
    this.dayOfMonth = dayOfMonth;
  }

  public String getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(String dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  private String scheduleInputsToString() {
    String min = this.min;
    String hour = this.hour;
    String dayOfMonth = this.dayOfMonth.equals("0") ? "*" : this.dayOfMonth;
    String month = this.month.equals("0") ? "*" : this.month;
    String dayOfWeek = this.dayOfWeek;
    this.min = this.hour = this.dayOfMonth = this.month = this.dayOfWeek = null;
    if (dayOfMonth != null && month != null && dayOfWeek != null) {
      return min + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek;
    } else {
      logger.log(Level.ERROR, "Something messed up with the schedule inputs.");
    }
    return null;
  }

  private void scheduleStringToInputs(String scheduleString) {
    if (scheduleString != null) {
      String[] inputs = scheduleString.split(" +");
      if (inputs.length == 5) {
	min = inputs[0];
	hour = inputs[1];
	dayOfMonth = inputs[2].equals("*") ? "0" : inputs[2];
	month = inputs[3].equals("*") ? "0" : inputs[3];
	dayOfWeek = inputs[4];
      } else {
	logger.log(Level.ERROR, "Something messed up with the persisted schedule string ("
	    + scheduleString + ").");
      }
    } else {
      this.dayOfMonth = this.month = this.dayOfWeek = null;
      this.min = "0";
      this.hour = "12";
    }
  }

  public List<SelectItem> getMonths() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    for (Month month : Month.values()) {
      SelectItem selectItem = new SelectItem();
      String key = month.name();
      Integer value = month.ordinal();
      selectItem.setLabel(key);
      selectItem.setValue(value);
      list.add(selectItem);
    }
    return list;
  }

  public List<SelectItem> getDaysOfWeek() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    for (DayOfTheWeek day : DayOfTheWeek.values()) {
      SelectItem selectItem = new SelectItem();
      selectItem.setLabel(day.name());
      selectItem.setValue(day.fieldValue());
      list.add(selectItem);
    }
    return list;
  }

  public List<SelectItem> getHours() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    for (int i = 0; i <= 24; i++) {
      SelectItem selectItem = new SelectItem();
      String key = String.valueOf(i);
      Integer value = i;
      selectItem.setLabel(key);
      selectItem.setValue(value);
      list.add(selectItem);
    }
    return list;
  }

  public List<SelectItem> getMins() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    for (int i = 0; i < 60; i++) {
      SelectItem selectItem = new SelectItem();
      String key = String.valueOf(i);
      Integer value = i;
      selectItem.setLabel(key);
      selectItem.setValue(value);
      list.add(selectItem);
    }
    return list;
  }

  public List<SelectItem> getDaysOfMonth() {
    List<SelectItem> list = new ArrayList<SelectItem>();
    for (int i = 0; i < 31; i++) {
      SelectItem selectItem = new SelectItem();
      String key = i == 0 ? "Any" : String.valueOf(i);
      Integer value = i;
      selectItem.setLabel(key);
      selectItem.setValue(value);
      list.add(selectItem);
    }
    return list;
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
    return listResources();
  }

  public String prev() {
    firstItem -= batchSize;
    if (firstItem < 0) {
      firstItem = 0;
    }
    return listResources();
  }

  public String listResources() {
    resources = null;
    itemCount = -1;
    return "list_resources";
  }
  
  public boolean isDescending() {
    return !isAsc;
  }
  
  public String getSortKey() {
    return sortKey;
  }
  
  public String sortByName() {
    sortKey = "name";
    isAsc = !isAsc;
    return listResources();
  }
  
  public String sortByStatus() {
    sortKey = "currentStatus";
    isAsc = !isAsc;
    return listResources();
  }
  
  public String sortByLastHarvest() {
    sortKey = "lastHarvestFinished,lastHarvestStarted";
    isAsc = !isAsc;
    return listResources();
  }

  public String sortByNextHarvest() {
    sortKey = "nextHarvestSchedule";
    isAsc = !isAsc;
    return listResources();
  }

  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="DAO methods">
  /* add new resource */
  public String prepareOaiPmhResourceToAdd() {
    logger.info("about to prepare oai=pmh resource to add");
    resource = new OaiPmhResource();
    resource.getClass().getSimpleName();
    return "new_OaiPmhResource";
  }

  public String prepareConnectorResourceToAdd() {
    resource = new HarvestConnectorResource();
    return "new_HarvestConnectorResource";
  }

  public String prepareWebCrawlResourceToAdd() {
    resource = new WebCrawlResource();
    return "new_WebCrawlResource";
  }

  public String prepareXmlBulkResourceToAdd() {
    resource = new XmlBulkResource();
    return "new_XmlBulkResource";
  }

  public String addResource() {
    prePersist();
    dao.create(resource);
    resource = null;
    return listResources();
  }

  /* update resource */
  public String prepareResourceToEdit() {
    logger.info("prepare resource to debug called..................");
    resource = getResourceFromRequestParam();
    postDePersist();
    logger.log(Level.INFO, "Retrieved persisted resource of type " + resource.getClass().getSimpleName());
    String type = resource.getClass().getSimpleName();
    return "edit_" + type;
/*
    if (resource instanceof OaiPmhResource) {
    } else if (resource instanceof WebCrawlResource) {
      return "edit_webcrawl";
    } else if (resource instanceof XmlBulkResource) {
      return "edit_xmlbulk";
    } else {
      logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
      return "failure";
    }
*/
  }

  /* update resource */
  public String prepareResourceToRun() {
    resource = getResourceFromRequestParam();
    String action = getParameterFromRequestParam("action");
    if (resource != null) { 
      if (!resource.getCurrentStatus().equals("RUNNING")) {
	resource.setHarvestImmediately(true);
	resource.setLastUpdated(new Date());
	resource = dao.update(resource);
	if (!"run".equals(action))
	  logger.warn("Got " + action + " on already running harvester job (" + resource.getId() + "). Expected run.");
      }
      else {
	// Just edit the records should stop it 
	resource.setLastUpdated(new Date());
	resource = dao.update(resource);
	if (!"stop".equals(action))
	  logger.warn("Got " + action + " on already running harvester job (" + resource.getId() + "). Expected stop.");
      }
    } else {
      logger.error("No resource found to start/stop");
    }
    return listResources();
  }

  public String saveResource() {
    prePersist();
    resource = dao.update(resource);
    resource = null;
    return listResources();
  }

  private boolean isPb() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
  }

  /* list resources */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public DataModel getResources() {
    // check if new request
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (resources == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
      req.setAttribute("listRequestSeen", "yes");
      resources = (List) dao.retrieveBriefs(firstItem, batchSize, sortKey, isAsc);
    }
    return new ListDataModel(resources);
  }

  public String deleteResource() {
    resource = getResourceFromRequestParam();
    dao.delete(resource);
    resource = null;
    return listResources();
  }

  public String saveAndPurge() {
    dao.delete(resource);
    prePersist();
    resource.setId(null);
    dao.create(resource);
    resource = null;
    return listResources();
  }

  private void prePersist() {
    resource.setScheduleString(scheduleInputsToString());
    if (resource instanceof OaiPmhResource) {
      if (longDate)
	((OaiPmhResource) resource).setDateFormat(LONG_DATE_FORMAT);
      else
	((OaiPmhResource) resource).setDateFormat(SHORT_DATE_FORMAT);
    }
    resource.setLastUpdated(new Date());
  }

  private void postDePersist() {
    if (resource.getScheduleString() != null) {
      scheduleStringToInputs(resource.getScheduleString());
    }
    if (resource instanceof OaiPmhResource) {
      if (((OaiPmhResource) resource).getDateFormat().equals(LONG_DATE_FORMAT))
	longDate = true;
      else
	longDate = false;
    }
  }

  @SuppressWarnings("unused")
  public String viewJobLog() {
    String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
	.get("resourceId");
    String start = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
	.get("start");
    String end = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
	.get("end");
    Long id = new Long(param);
    setCurrentId(id);

    StringBuilder sb = new StringBuilder(10240);
    InputStream is = dao.getLog(id);
    /* This check should no longer be in use: Implemented in the web service */ 
    Reader tempReader;
    if (is == null) {
      tempReader = new StringReader("--- WARNING: No Job Log found ---"); 
    }
    else 
      tempReader = new InputStreamReader(is);
    BufferedReader reader = new BufferedReader(tempReader);
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
	sb.append(line + "\n");
      }
    } catch (IOException e) {
      logger.log(Level.ERROR, e);
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (IOException e) {
	logger.log(Level.ERROR, e);
      }
    }
    jobLog = sb.toString();
    return "harvester_log";

  }

  public String getjobLog() {
    return jobLog;
  }

  // </editor-fold>

  /* parameter value from request */
  public String getParameterFromRequestParam(String name) {
      String value = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get(name);
      return value;
    }

  /* objects from request */
  public Harvestable getResourceFromRequestParam() {
    Harvestable o = null;
    if (model != null) {
      o = (Harvestable) model.getRowData();
      // o = em.merge(o);
    } else {
      String param = getParameterFromRequestParam("resourceId");
      Long id = new Long(param);
      o = dao.retrieveById(id);
    }
    return o;
  }

  
  /* objects from request */
/*
  public String getActionFromRequestParam() {
      String param = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get("action");
    return param;
  }
*/
  
  public String getTransformation() {
    if (resource != null && resource.getTransformation() != null)
      return resource.getTransformation().getId().toString();
    return "";
  }

  public void setTransformation(String transformationId) {
    try {
      if (transformationId != null && resource != null) {
	if (transformationId.equals(""))
	  resource.setTransformation(null);
	else {
	  Long id = new Long(transformationId);
	  TransformationDAO transformationDAO = TransformationDAOFactory
	      .getTransformationDAO((ServletContext) FacesContext.getCurrentInstance()
		  .getExternalContext().getContext());
	  Transformation transformation = transformationDAO.retrieveById(id);
	  if (transformation == null) {
	    logger.warn("No Transformation found for ID: " + id);
	  }
	  resource.setTransformation(transformation);
	}
      }
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when updating Storage", ex);
      ex.printStackTrace();
    }

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

  public String getStorage() {
    if (resource != null && resource.getStorage() != null)
      return resource.getStorage().getId().toString();
    else
      return "";
  }

  public void setStorage(String storage) {
    try {
      if (storage != null && resource != null) {
	Long id = new Long(storage);
	StorageDAO storageDAO = StorageDAOFactory.getStorageDAO((ServletContext) FacesContext
	    .getCurrentInstance().getExternalContext().getContext());
	resource.setStorage(storageDAO.retrieveById(id));
      }
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when updating Storage", ex);
      ex.printStackTrace();
    }
  }

  public Long getCurrentId() {
    return currentId;
  }

  public void setCurrentId(Long currentId) {
    this.currentId = currentId;
  }

}
