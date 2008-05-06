/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.controller;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.entity.OaiPmhResource;
import com.indexdata.localindexes.web.entity.WebCrawlResource;
import com.indexdata.localindexes.web.entity.XmlBulkResource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.persistence.*;
import javax.transaction.UserTransaction;

/**
 *
 * @author jakub
 */
public class ResourceController {
    /*@PersistenceContext(name="localindexes")
    private EntityManager eM;*/

    @PersistenceUnit(unitName = "localindexes")
    private EntityManagerFactory emf;

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    @Resource
    private UserTransaction utx;
    private Harvestable resource;
    private DataModel model;

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
    private String month;
    private String dayOfWeek;
    private String dayOfMonth;

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
        String dayOfMonth = this.dayOfMonth.equals("0") ? "*" : this.dayOfMonth;
        String month = this.month.equals("0") ? "*" : this.month;
        String dayOfWeek = this.dayOfWeek.equals("0") ? "*" : this.dayOfWeek;
        this.dayOfMonth = this.month = this.dayOfWeek = null;
        if (dayOfMonth != null && month != null && dayOfWeek != null) {
            return "0" + " " + "0" + " " + dayOfMonth + " " + month + " " + dayOfWeek;
        } else {
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Something messed up with the schedule inputs.");
            return null;
        }
    }

    private void scheduleStringToInputs(String scheduleString) {
        if (scheduleString != null) {
            String[] inputs = scheduleString.split(" +");
            if (inputs.length == 5) {
                dayOfMonth = inputs[2].equals("*") ? "0" : inputs[2];
                month = inputs[3].equals("*") ? "0" : inputs[3];
                dayOfWeek = inputs[4].equals("*") ? "0" : inputs[4];
            } else {
                Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Something messed up with the persisted schedule string (" + scheduleString + ").");
            }
        } else {
            this.dayOfMonth = this.month = this.dayOfWeek = null;
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
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Harvestable as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }
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
        resource.setScheduleString(scheduleInputsToString());
        EntityManager eM = getEntityManager();
        try {
            utx.begin();
            eM.joinTransaction();
            eM.persist(resource);
            utx.commit();
            addSuccessMessage("Resource was successfully added.");
        } catch (Exception e) {
            addErrorMessage(e.getLocalizedMessage());
            try {
                utx.rollback();
            } catch (Exception e2) {
                addErrorMessage(e2.getLocalizedMessage());
                return "failure";
            }
            return "failure";
        } finally {
            eM.close();
        }
        return "resource_added";
    }

    /* update resource */
    public String prepareResourceToEdit() {
        resource = getResourceFromRequestParam();
        scheduleStringToInputs(resource.getScheduleString());
        addSuccessMessage("Retrieved persisted resource of type " + resource.getClass().getName());
        if (resource instanceof OaiPmhResource) {
            return "edit_oaipmh";
        } else if (resource instanceof WebCrawlResource) {
            return "edit_webcrawl";
        } else if (resource instanceof XmlBulkResource) {
            return "edit_xmlbulk";
        } else {
            addErrorMessage("Unknonw resource type. No matching form defined.");
            return "failure";
        }
    }

    public String saveResource() {
        resource.setScheduleString(scheduleInputsToString());
        EntityManager em = getEntityManager();
        try {
            utx.begin();
            em.joinTransaction();
            resource = em.merge(resource);
            utx.commit();
            addSuccessMessage("Resource was successfully updated.");
        } catch (Exception ex) {
            try {
                addErrorMessage(ex.getLocalizedMessage());
                utx.rollback();
            } catch (Exception e) {
                addErrorMessage(e.getLocalizedMessage());
            }
        } finally {
            em.close();
        }
        return "resource_saved";
    }

    /* list resources */
    public DataModel getResources() {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("select object(o) from Harvestable as o");
            q.setMaxResults(batchSize);
            q.setFirstResult(firstItem);
            model = new ListDataModel(q.getResultList());
            return model;
        } finally {
            em.close();
        }
    }

    public String deleteResource() {
        EntityManager em = getEntityManager();
        try {
            utx.begin();
            em.joinTransaction();
            Harvestable resource = getResourceFromRequestParam();
            resource = em.merge(resource);
            em.remove(resource);
            utx.commit();
            addSuccessMessage("Resource was successfully deleted.");
        } catch (Exception ex) {
            try {
                addErrorMessage(ex.getLocalizedMessage());
                utx.rollback();
            } catch (Exception e) {
                addErrorMessage(e.getLocalizedMessage());
            }
        } finally {
            em.close();
        }
        return "resource_list";
    }

    /* objects from request */
    public Harvestable getResourceFromRequestParam() {
        EntityManager em = getEntityManager();
        try {
            Harvestable o = null;
            if (model != null) {
                o = (Harvestable) model.getRowData();
                o = em.merge(o);
            } else {
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
                Integer id = new Integer(param);
                o = em.find(Harvestable.class, id);
            }
            return o;
        } finally {
            em.close();
        }
    }

    /* logging messages */
    public static void addSuccessMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage("successInfo", facesMsg);
    }

    public static void addErrorMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(null, facesMsg);
    }
}
