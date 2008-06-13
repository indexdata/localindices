/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOException;
import com.indexdata.masterkey.localindices.dao.HarvestableDAOFactory;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;

/**
 * The cotroller for the admin interface, implements all the buisness logic and
 * controlls data access through DAO object
 * @author jakub
 */
public class ResourceController {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.web.admin");
    private HarvestableDAO dao;
    private Harvestable resource;
    private DataModel model;
    private Boolean longDate;
    private final static String SHORT_DATE_FORMAT = "yyyy-MM-dd";
    private final static String LONG_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";

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
            logger.log(Level.SEVERE, "Exception when retrieving DAO", ex);
        }
    }

    public Harvestable getResource() {
        return resource;
    }

    public void setResource(Harvestable resource) {
        this.resource = resource;
    }

    // <editor-fold defaultstate="collapsed" desc="Harvest schedule handling functions">
    private enum Month {

        Any, January, Febraruary, March, April, May, June,
        July, August, September, November, October, December
    }
    private enum DayOfTheWeek {

        Any, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
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
        String dayOfWeek = this.dayOfWeek.equals("0") ? "*" : this.dayOfWeek;
        this.min = this.hour = this.dayOfMonth = this.month = this.dayOfWeek = null;
        if (dayOfMonth != null && month != null && dayOfWeek != null) {
            return min + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek;
        } else {
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Something messed up with the schedule inputs.");
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
                dayOfWeek = inputs[4].equals("*") ? "0" : inputs[4];
            } else {
                Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Something messed up with the persisted schedule string (" + scheduleString + ").");
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
            String key = day.name();
            Integer value = day.ordinal();
            selectItem.setLabel(key);
            selectItem.setValue(value);
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
    private int batchSize = 10;

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
        return dao.getHarvestableCount();
    }

    public String next() {
        if (firstItem + batchSize < getItemCount()) {
            firstItem += batchSize;
        }
        return "next_resource_batch";
    }

    public String prev() {
        firstItem -= batchSize;
        if (firstItem < 0) {
            firstItem = 0;
        }
        return "prev_resource_batch";
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

    public String addResource() {
        prePersist();
        dao.createHarvestable(resource);
        resource = null;
        //return failure
        return "resource_added";
    }

    /* update resource */
    public String prepareResourceToEdit() {
        resource = getResourceFromRequestParam();
        scheduleStringToInputs(resource.getScheduleString());
        logger.log(Level.INFO, "Retrieved persisted resource of type " + resource.getClass().getName());
        if (resource instanceof OaiPmhResource) {
            return "edit_oaipmh";
        } else if (resource instanceof WebCrawlResource) {
            return "edit_webcrawl";
        } else if (resource instanceof XmlBulkResource) {
            return "edit_xmlbulk";
        } else {
            logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
            return "failure";
        }
    }

    public String saveResource() {
        prePersist();
        resource = dao.updateHarvestable(resource);
        resource = null;
        return "resource_saved";
    }

    /* list resources */
    public DataModel getResources() {
        return new ListDataModel((List) dao.retrieveHarvestables(firstItem, batchSize));
    }

    public String deleteResource() {
        resource = getResourceFromRequestParam();
        dao.deleteHarvestable(resource);
        resource = null;
        return "resource_list";
    }
    
    public String saveAndPurge() {
        dao.deleteHarvestable(resource);
        prePersist();
        resource.setId(null);
        dao.createHarvestable(resource);
        resource = null;
        return "resource_saved";
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
        scheduleStringToInputs(resource.getScheduleString());
        if (resource instanceof OaiPmhResource) {
            if (((OaiPmhResource) resource).getDateFormat().equals(LONG_DATE_FORMAT))
                    longDate = true;
            else
                longDate = false;
        }
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
}
