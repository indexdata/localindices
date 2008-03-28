/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.controllers;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import com.indexdata.localindexes.web.entitybeans.OaiPmhResource;
import com.indexdata.localindexes.web.entitybeans.WebCrawlResource;
import com.indexdata.localindexes.web.entitybeans.XmlBulkResource;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
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

    public String addEditedResource() {
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
    
    public String prepareResourceToEdit () {
        resource = getResourceFromRequestParam();
        return "edit_resource";
    }
    
    public String saveEditedResource() {
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
