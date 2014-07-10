/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityInUse;
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
import com.indexdata.masterkey.localindices.entity.StatusResource;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.utils.CronLine;
import com.indexdata.utils.CronLineParseException;
import com.indexdata.utils.ISOLikeDateParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.TimeZone;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The controller for the Admin interface, implements all the business logic and
 * controls data access through DAO object
 * 
 * @author jakub
 * @author Dennis
 */
public class JobController {
  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  private HarvestableDAO dao;
  private Harvestable resource;
  @SuppressWarnings("rawtypes")
  private DataModel model;
  private Boolean longDate;
  public final static String SHORT_DATE_FORMAT = "yyyy-MM-dd";
  public final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
  @SuppressWarnings("rawtypes")
  private List resources;
  private String latestLogEntries;
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

  public JobController() {
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

  private String scheduleInputsToString() throws ContinueEdit {
    String min = this.min;
    String hour = this.hour;
    String dayOfMonth = this.dayOfMonth.equals("0") ? "*" : this.dayOfMonth;
    String month = this.month.equals("0") ? "*" : this.month;
    String dayOfWeek = this.dayOfWeek;
    this.min = this.hour = this.dayOfMonth = this.month = this.dayOfWeek = null;
    if (dayOfMonth != null && month != null && dayOfWeek != null) {
      String cronLine = min + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek;
      try {
        CronLine cron = new CronLine(cronLine);
	cron.nextMatchingDate(new Date());
	return cronLine;
      } catch (CronLineParseException clpe) {
	logger.log(Level.ERROR, "Failed to find next schedule from " + cronLine);
	FacesContext.getCurrentInstance().addMessage("jobForm:schedule", new FacesMessage(clpe.getMessage()));
      }

    } else {
      logger.log(Level.ERROR, "Something messed up with the schedule inputs.");
      FacesContext.getCurrentInstance().addMessage("jobForm:schedule", new FacesMessage("Schedule did not resolve to a real date (internal error)"));
    }
    throw new ContinueEdit();
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
    for (int i = 0; i < 32; i++) {
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
    int lastItem = (count < firstItem + batchSize) ? count : firstItem + batchSize;
    return lastItem;
  }

  public int getItemCount() {
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (itemCount == -1 || req.getAttribute("countRequestSeenFlag") == null) {
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
    isAsc = "name".equals(sortKey) ? !isAsc : true;
    sortKey = "name";
    return listResources();
  }
  
  public String sortByStatus() {
    isAsc = "currentStatus".equals(sortKey) ? !isAsc : true;
    sortKey = "currentStatus";
    return listResources();
  }
  
  public String sortByLastHarvest() {
    isAsc = "lastHarvestStartedOrFinished".equals(sortKey) ? !isAsc : true;
    sortKey = "lastHarvestStartedOrFinished";
    return listResources();
  }

  public String sortByNextHarvest() {
    isAsc = "nextHarvestSchedule".equals(sortKey) ? !isAsc : true;
    sortKey = "nextHarvestSchedule";
    return listResources();
  }

  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="DAO methods">
  
  public void prepareOaiPmhResourceToAdd() {
    //avoid resetting on partial updates (ajax)
    if (!FacesContext.getCurrentInstance().isPostback())
      resource = new OaiPmhResource();
  }

  public void prepareConnectorResourceToAdd() {
    //avoid resetting on partial updates (ajax)
    if (!FacesContext.getCurrentInstance().isPostback())
      resource = new HarvestConnectorResource();
  }

  public void prepareWebCrawlResourceToAdd() {
    //avoid resetting on partial updates (ajax)
    if (!FacesContext.getCurrentInstance().isPostback())
      resource = new WebCrawlResource();
  }

  public void prepareXmlBulkResourceToAdd() {
    //avoid resetting on partial updates (ajax)
    if (!FacesContext.getCurrentInstance().isPostback())
      resource = new XmlBulkResource();
  }

  public void prepareStatusResourceToAdd() {
    //avoid resetting on partial updates (ajax)
    if (!FacesContext.getCurrentInstance().isPostback())
      resource = new StatusResource();
  }

  private String createResource() {
    try {
    prePersist();
    dao.create(resource);
    resource = null;
    return listResources();
    } catch (ContinueEdit ex) {
      String type = resource.getClass().getSimpleName();
      return "edit_" + type;
    }
  }
  
  public String addResource() {
    resource.setHarvestImmediately(false);
    return createResource();
  }
  
  public String addRunResource() {
    resource.setHarvestImmediately(true);
    return createResource();
  }
  
  public String addAndRunFromCache() {
    resource.setDiskRun(true);
    return addRunResource();
    
  }
  
  public void prepareResourceToEdit() {
    //avoid retrieving instances on partial (ajax) updates
    if (!FacesContext.getCurrentInstance().isPostback()) {
      resource = getResourceFromRequestParam();
      postDePersist();
    }
  }

  /* update resource */
  public String prepareResourceToRun() {
    resource = getResourceFromRequestParam();
    if (resource == null) {
      logger.error("No resource found in the request, ignoring action.");
      return listResources();
    }
    String action = getRequestParam("action");
    if (action == null) {
      logger.error("Received 'null' action, ignoring");
      return listResources();
    }
    if ("run".equals(action)) {
      if (resource.getCurrentStatus().equals("RUNNING")) {
        logger.warn("Trying to run already running resource, ignoring");
        return listResources();
      }
      resource.setLastUpdated(new Date());
      resource.setHarvestImmediately(true);
      resource.setDiskRun(false);
      dao.update(resource);
    } else if ("run_cached".equals(action)) {
      if (resource.getCurrentStatus().equals("RUNNING")) {
        logger.warn("Trying to run already running resource, ignoring");
        return listResources();
      }
      resource.setLastUpdated(new Date());
      resource.setHarvestImmediately(true);
      resource.setDiskRun(true);
      dao.update(resource);
    } else if ("stop".equals(action)) {
      resource.setLastUpdated(new Date());
      dao.update(resource);
    } else {
      logger.warn("Unknown action '"+action+"'");
    }
    return listResources();
  }
  
  private String updateResource() {
    try {
      prePersist();
      resource = dao.update(resource);
      resource = null;
      return listResources();
    } catch (ContinueEdit ce) {
      String type = resource.getClass().getSimpleName();
      return "edit_" + type;
    }
  }

  public String saveResource() {
    resource.setHarvestImmediately(false);
    return updateResource();
  }

  public String runResource() {
    resource.setHarvestImmediately(true);
    return updateResource();
  }
  
  public String runFromCache() {
    resource.setDiskRun(true);
    return runResource();
  }

  // TODO change that to fire off on preRenderView
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public DataModel getResources() {
    // check if new request
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (resources == null || req.getAttribute("listRequestSeen") == null) {
      req.setAttribute("listRequestSeen", "yes");
      resources = (List) dao.retrieveBriefs(firstItem, batchSize, sortKey, isAsc);
    }
    return new ListDataModel(resources);
  }

  public String deleteResource() {
    resource = getResourceFromRequestParam();
    try {
      dao.delete(resource);
    } catch (EntityInUse eiu) {
      logger.warn("Cannot remove job", eiu);
    }
    resource = null;
    return listResources();
  }

  public String getStorageUrl() {
    if (resource != null && resource.getId() != null && resource.getStorage() != null) {
      return resource.getStorage().getSearchUrl(resource);
    }
    return null;
  }

  public String saveAndPurge() {
    try {
      dao.delete(resource);
    } catch (EntityInUse eiu) {
      logger.warn("Job is in use", eiu);
    }
    try {
      prePersist();
      resource.setId(null);
      dao.create(resource);
      resource = null;
      return listResources();
    } catch (ContinueEdit ce) {
      String type = resource.getClass().getSimpleName();
      return "edit_" + type;
    }
  }

  public String reset() {
    resource.setLastUpdated(new Date());
    dao.reset(resource.getId());
    // Reload resource. 
    resource = dao.retrieveById(resource.getId());
    String type = resource.getClass().getSimpleName();
    return "edit_" + type;
    //resource = null;
    //return listResources();
  }
  
  public String resetCache() {
    try {
      dao.resetCache(resource.getId());
    } catch (DAOException daoe) {
      logger.error("Resetting cache failed", daoe);
      return "failure";
    }
    return null;
  }

  private void prePersist() throws ContinueEdit {
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
  
  private Date lastEntry;
  //TODO keep this in one place in case log format changes
  private final static int dateFieldLength = "YYYY-mm-dd HH:mm:ss,SSS".length();
  
  public void prepareJobLog() {
    resource = getResourceFromRequestParam();
    lastEntry = resource.getLastHarvestStarted();
    fetchLogEntries();
  }

  public String fetchLogEntries() {
    InputStream is;
    try {
      is = dao.getLog(resource.getId(), lastEntry);
    } catch (DAOException ex) {
      logger.error("Error retrieving logfile", ex);
      return "failure";
    }
    if (is == null) {
      latestLogEntries = "";
      return "harvester_log";
    }
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;
    String lastLine = null;
    try {
      while ((line = reader.readLine()) != null) {
	sb.append(line).append("\n");
        lastLine = line;
      }
    } catch (IOException e) {
      logger.error("Error reading logfile", e);
    } finally {
      try {
        is.close();
      } catch (IOException ex) {
        logger.warn("Error closing log stream", ex);
      }
    }
    //parse the last log date, this is more reliable than local time
    if (lastLine != null) {
      if (lastLine.length() > dateFieldLength) {
        try {
          //log entries are in the harvester local times zone
          //we assume the same time zone as harvester admin
          //TODO ask the harvester for the time zone
          TimeZone tz = TimeZone.getDefault();
          lastEntry = ISOLikeDateParser.parse(lastLine.substring(0,dateFieldLength), tz);
        } catch (ParseException pe) {
          //it's possible that we are unlucky and the lastline does not have a timestamp
          //we assume current date
          lastEntry = new Date();
        }
      } else {
        lastEntry = new Date();
      }
    }
    latestLogEntries = sb.toString();
    return "harvester_log";
  }
  
  public String getLatestLogEntries() {
    return latestLogEntries;
  }

  // </editor-fold>

  private String getRequestParam(String name) {
      String value = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get(name);
      return value;
    }

  //TODO avoid a back-trip to the web-service when job is already retrieved
  public Harvestable getResourceFromRequestParam() {
    Harvestable o = null;
    if (model != null) {
      o = (Harvestable) model.getRowData();
      // o = em.merge(o);
    } else {
      String param = getRequestParam("resourceId");
      if (param == null || param.isEmpty())
        return null;
      Long id = new Long(param);
      o = dao.retrieveById(id);
    }
    return o;
  }
  
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

  String[] logLevels = {"DEBUG", "INFO", "WARN", "ERROR"};
  public List<SelectItem> getLogLevelItems() 
  {
    List<SelectItem> list = new LinkedList<SelectItem>();
    for (String logLevel : logLevels) {
      SelectItem selectItem = new SelectItem();
      selectItem.setLabel(logLevel);
      selectItem.setValue(logLevel);
      list.add(selectItem);
    }
    return list;
  }

  String[] jobLevels = {"OK", "WARN", "ERROR" };
  public List<SelectItem> getJobLevelItems() {
    List<SelectItem> list = new LinkedList<SelectItem>();
    for (String logLevel : jobLevels) {
      SelectItem selectItem = new SelectItem();
      selectItem.setLabel(logLevel);
      selectItem.setValue(logLevel);
      list.add(selectItem);
    }
    return list;
  }

}
