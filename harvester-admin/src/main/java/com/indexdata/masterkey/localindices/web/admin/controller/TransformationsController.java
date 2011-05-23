/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAOFactory;
import com.indexdata.masterkey.localindices.entity.Transformation;

/**
 * The controller for the Admin interface for Transformations, implements all the business logic and
 * controls data access through DAO object
 * @author Dennis 
 */
public class TransformationsController {
    private Logger logger = Logger.getLogger(getClass());
    // Transformation
    private TransformationDAO dao;
    private Transformation resource;
    
    private DataModel model;
    @SuppressWarnings("rawtypes")
	private List resources;
    
    public TransformationsController() {
        try {
            dao = TransformationDAOFactory.getTransformationDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext() );
        } catch (DAOException ex) {
            logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
        }
    }

    public Transformation getResource() {
        return resource;
    }
    public void setResource(Transformation resource) {
        this.resource = resource;
    }
    
    
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Transformation list paging functions">
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
            itemCount = dao.getTransformationCount();
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
        return "list_Transformations";
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="DAO methods">
    /* add new resource */
    public String prepareSolrTransformationToAdd() {
        // TODO 
    	// resource = new SolrTransformation();
        return "new_solrTransformation";
    }

    /* TODO Fix */
    public String prepareZebraTransformationToAdd() {
        // TODO 
    	// resource = new SolrTransformation();
        return "new_zebraTransformation";
    }

    public String prepareXmlTransformationToAdd() {
        // TODO 
    	// resource = new SolrTransformation();
        return "new_xmlTransformation";
    }

    public String prepareConsoleTransformationToAdd() {
        // TODO 
    	// resource = new SolrTransformation();
        return "new_consoleTransformation";
    }

    
    public String addResource() {
        prePersist();
        dao.createTransformation(resource);
        resource = null;
        return listResources();
    }

    /* update resource */
    public String prepareResourceToEdit() {
        resource = getResourceFromRequestParam();
        postDePersist();
        logger.log(Level.INFO, "Retrieved persisted resource of type " + resource.getClass().getName());
        if (resource instanceof Transformation) {
            return "edit_transformation";
        } 
        /* 
        else if (resource instanceof ZebraTransformation) {
            return "edit_zebraTransformation";
        } else if (resource instanceof ConsoleTransformation) {
            return "edit_console";
        } else if (resource instanceof XmlTransformation) {
            return "edit_xmlTransformation";
        } */
        else {
            logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
            return "failure";
        }
    }

    public String saveResource() {
        prePersist();
        resource = dao.updateTransformation(resource);
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
            resources = (List) dao.retrieveTransformationBriefs(firstItem, batchSize);
        }
        if (resources != null)
            Collections.sort(resources);
        return new ListDataModel(resources);
    }

    public String deleteResource() {
        resource = getResourceFromRequestParam();
        dao.deleteTransformation(resource);
        resource = null;
        return listResources();
    }
    
    public String saveAndPurge() {
        dao.deleteTransformation(resource);
        prePersist();
        resource.setId(null);
        dao.createTransformation(resource);
        resource = null;
        return listResources();
    }
    
    private void prePersist() {        

    }

    private void postDePersist() {

    }

    //</editor-fold>

    /* objects from request */
    public Transformation getResourceFromRequestParam() {
            Transformation o = null;
            if (model != null) {
                o = (Transformation) model.getRowData();
                //o = em.merge(o);
            } else {
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
                Long id = new Long(param);
                o = dao.retrieveTransformationById(id);
            }
            return o;
    }
}
