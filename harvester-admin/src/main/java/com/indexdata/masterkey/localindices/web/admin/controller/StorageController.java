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
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAOFactory;
import com.indexdata.masterkey.localindices.entity.SolrStorage;
import com.indexdata.masterkey.localindices.entity.Storage;

/**
 * The controller for the Admin interface for storages, implements all the business logic and
 * controls data access through DAO object
 * @author Dennis 
 */
public class StorageController {
    private Logger logger = Logger.getLogger(getClass());
    // Storage
    private StorageDAO dao;
    private Storage resource;
    
    private DataModel model;
    @SuppressWarnings("rawtypes")
	private List resources;
    
    public StorageController() {
        try {
            dao = StorageDAOFactory.getStorageDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
        } catch (DAOException ex) {
            logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
        }
    }

    public Storage getResource() {
        return resource;
    }
    public void setResource(Storage resource) {
        this.resource = resource;
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
            itemCount = dao.getStorageCount();
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
    public String prepareSolrStorageToAdd() {
        resource = new SolrStorage();
        return "new_solrstorage";
    }

    /* TODO Fix */
    public String prepareZebraStorageToAdd() {
        resource = new SolrStorage();
        return "new_zebrastorage";
    }

    public String prepareXmlStorageToAdd() {
        resource = new SolrStorage();
        return "new_xmlstorage";
    }

    public String prepareConsoleStorageToAdd() {
        resource = new SolrStorage();
        return "new_consolestorage";
    }

    
    public String addResource() {
        prePersist();
        dao.createStorage(resource);
        resource = null;
        return listResources();
    }

    /* update resource */
    public String prepareResourceToEdit() {
        resource = getResourceFromRequestParam();
        postDePersist();
        logger.log(Level.INFO, "Retrieved persisted resource of type " + resource.getClass().getName());
        if (resource instanceof SolrStorage) {
            return "edit_solr";
        } 
        /* 
        else if (resource instanceof ZebraStorage) {
            return "edit_zebrastorage";
        } else if (resource instanceof ConsoleStorage) {
            return "edit_console";
        } else if (resource instanceof XmlStorage) {
            return "edit_xmlstorage";
        } */
        else {
            logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
            return "failure";
        }
    }

    public String saveResource() {
        prePersist();
        resource = dao.updateStorage(resource);
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
            resources = (List) dao.retrieveStorageBriefs(firstItem, batchSize);
        }
        if (resources != null)
            Collections.sort(resources);
        return new ListDataModel(resources);
    }

    public String deleteResource() {
        resource = getResourceFromRequestParam();
        dao.deleteStorage(resource);
        resource = null;
        return listResources();
    }
    
    public String saveAndPurge() {
        dao.deleteStorage(resource);
        prePersist();
        resource.setId(null);
        dao.createStorage(resource);
        resource = null;
        return listResources();
    }
    
    private void prePersist() {        

    }

    private void postDePersist() {

    }

    private String jobLog;

    public String viewJobLog() {
        String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
        Long id = new Long(param);
        //slurp that damn log to string, before I figure how to cleanly get handle
        //of the InputStream in the view
        StringBuilder sb = new StringBuilder(1024);
        InputStream is = dao.getStorageLog(id);
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
    public Storage getResourceFromRequestParam() {
            Storage o = null;
            if (model != null) {
                o = (Storage) model.getRowData();
                //o = em.merge(o);
            } else {
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("resourceId");
                Long id = new Long(param);
                o = dao.retrieveStorageById(id);
            }
            return o;
    }
}
