/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.controllers;

import com.indexdata.localindexes.web.entitybeans.OaiPmhResource;
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

public class OaiPmhResourceController {
    /*@PersistenceContext(name="localindexes")
    private EntityManager eM;*/
    
    @PersistenceUnit(unitName = "localindexes")
    private EntityManagerFactory emf;

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    
    @Resource
    private UserTransaction utx;
    
    private OaiPmhResource resource = new OaiPmhResource();
    private DataModel model;

    public OaiPmhResource getResource() {
        return resource;
    }

    public void setResource(OaiPmhResource resource) {
        this.resource = resource;
    }
    
    /* paging */
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
        try{
            int count = ((Long) em.createQuery("select count(o) from OaiPmhResource as o").getSingleResult()).intValue();
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

    /** Creates a new instance of OaiPmhResourceController */
    public OaiPmhResourceController() {
    }
    
    public String addEditedResource() {
        EntityManager eM = getEntityManager();
        try {
            utx.begin();
            eM.joinTransaction();
            eM.persist(resource);
            utx.commit();
        } catch (Exception e) {
            //addErrorMessage(e.getLocalizedMessage());
            try { 
                utx.rollback();
            } catch (Exception e2) {
                return "failure";
               // addErrorMessage(e2.getLocalizedMessage());
            }
            return "failure";
        } finally {
            eM.close();
        }
        return "resource_added";
    }
    
    public DataModel getResources() {
        EntityManager em = getEntityManager();
        try{
            Query q = em.createQuery("select object(o) from OaiPmhResource as o");
            q.setMaxResults(batchSize);
            q.setFirstResult(firstItem);
            model = new ListDataModel(q.getResultList());
            return model;
        } finally {
            em.close();
        }
    }
    
    public static void addErrorMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(null, facesMsg);
    }

}
