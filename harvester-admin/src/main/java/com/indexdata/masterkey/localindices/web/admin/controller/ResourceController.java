/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOException;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOFactory;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAOFactory;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAOFactory;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.SolrXmlBulkResource;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

/**
 * The controller for the Admin interface, implements all the business logic and
 * controls data access through DAO object
 * @author jakub
 */
public class ResourceController {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
    private HarvestableDAO dao;
    private Harvestable resource;
    private DataModel model;
    private Boolean longDate;
    private final static String SHORT_DATE_FORMAT = "yyyy-MM-dd";
    private final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
    @SuppressWarnings("rawtypes")
	private List resources;
    private String jobLog;
	Stack<String> backActions = new Stack<String>();
	String homeAction = "home";

    
    public Boolean getLongDate() {
        return longDate;
    }
    public void setLongDate(Boolean longDate) {
        this.longDate = longDate;
    }
    
    public ResourceController() {
        try {
            dao = HarvestableDAOFactory.getHarvestableDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
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
    
    public List<SelectItem> getMetadataPrefixes() {
        List<SelectItem> list = new ArrayList<SelectItem>();
        
        SelectItem selectItem = new SelectItem();
        selectItem.setLabel("OAI_DC");
        selectItem.setValue("oai_dc");
        list.add(selectItem);
        
        selectItem = new SelectItem();
        selectItem.setLabel("MARC21/USMARC");
        selectItem.setValue("marc21");
        list.add(selectItem);

        selectItem = new SelectItem();
        selectItem.setLabel("PP2");
        selectItem.setValue("pp2-solr");
        list.add(selectItem);

        return list;
    }

    // <editor-fold defaultstate="collapsed" desc="Harvest schedule handling functions">
    private enum Month {

        Any, January, February, March, April, May, June,
        July, August, September, October, November, December
    }
    private enum DayOfTheWeek {
        Any("*"), Monday("1"), Tuesday("2"), Wednesday("3"), Thursday("4"), Friday("5"), Saturday("6"), Sunday("0");
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
                logger.log(Level.ERROR, "Something messed up with the persisted schedule string (" + scheduleString + ").");
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
    
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Resource list paging functions">
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
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (itemCount == -1 || !isPb() && req.getAttribute("countRequestSeenFlag") == null) {
            req.setAttribute("countRequestSeenFlag", "yes");
            itemCount = dao.getHarvestableCount();
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

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="DAO methods">
    /* add new resource */
    public String prepareOaiPmhResourceToAdd() {
        resource = new OaiPmhResource();
        return "new_oaipmh";
    }

    public String prepareWebCrawlResourceToAdd() {
        resource = new WebCrawlResource();
        return "new_webcrawl";
    }

    public String prepareXmlBulkResourceToAdd() {
        resource = new XmlBulkResource();
        return "new_xmlbulk";
    }

    public String prepareSolrBulkResourceToAdd() {
        resource = new SolrXmlBulkResource();
        return "new_solrxmlbulk";
    }

    
    public String addResource() {
        prePersist();
        dao.createHarvestable(resource);
        resource = null;
        return listResources();
    }

    /* update resource */
    public String prepareResourceToEdit() {
        resource = getResourceFromRequestParam();
        postDePersist();
        logger.log(Level.INFO, "Retrieved persisted resource of type " + resource.getClass().getName());
        if (resource instanceof OaiPmhResource) {
            return "edit_oaipmh";
        } else if (resource instanceof WebCrawlResource) {
            return "edit_webcrawl";
        } else if (resource instanceof XmlBulkResource) {
            return "edit_xmlbulk";
        } else if (resource instanceof SolrXmlBulkResource) {
            return "edit_solrxmlbulk";
        } else {
            logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
            return "failure";
        }
    }

    public String saveResource() {
        prePersist();
        resource = dao.updateHarvestable(resource);
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
        //check if new request
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (resources == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
            req.setAttribute("listRequestSeen", "yes");
            resources = (List) dao.retrieveHarvestableBriefs(firstItem, batchSize);
        }
        if (resources != null)
            Collections.sort(resources);
        return new ListDataModel(resources);
    }

    public String deleteResource() {
        resource = getResourceFromRequestParam();
        dao.deleteHarvestable(resource);
        resource = null;
        return listResources();
    }
    
    public String saveAndPurge() {
        dao.deleteHarvestable(resource);
        prePersist();
        resource.setId(null);
        dao.createHarvestable(resource);
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

    public String viewJobLog() {
        String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
        Long id = new Long(param);
        //slurp that damn log to string, before I figure how to cleanly get handle
        //of the InputStream in the view
        StringBuilder sb = new StringBuilder(1024);
        InputStream is = dao.getHarvestableLog(id);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            logger.log(Level.ERROR, e);
        } finally {
            try {
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
    //</editor-fold>

    /* objects from request */
    public Harvestable getResourceFromRequestParam() {
            Harvestable o = null;
            if (model != null) {
                o = (Harvestable) model.getRowData();
                //o = em.merge(o);
            } else {
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
                Long id = new Long(param);
                o = dao.retrieveHarvestableById(id);
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
					TransformationDAO transformationDAO = TransformationDAOFactory.getTransformationDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
					Transformation transformation = transformationDAO.retrieveTransformationById(id);
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
		            StorageDAO storageDAO = StorageDAOFactory.getStorageDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
		            resource.setStorage(storageDAO.retrieveStorageById(id));
			}
        } catch (DAOException ex) {
            logger.log(Level.FATAL, "Exception when updating Storage", ex);
            ex.printStackTrace();
        }
	}
	
}
