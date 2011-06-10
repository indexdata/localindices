/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.Collections;
import java.util.LinkedList;
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
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAOFactory;
import com.indexdata.masterkey.localindices.entity.SolrStorage;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;

/**
 * The controller for the Admin interface for storages, implements all the business logic and
 * controls data access through DAO object
 * @author Dennis 
 */
public class StorageController  {
    private Logger logger = Logger.getLogger(getClass());
    // Storage
    private StorageDAO dao;
    private Storage storage;
    
    private DataModel model;
	private List<StorageBrief> storages;
    private String jobLog;
	Stack<String> backActions = new Stack<String>();
	String homeAction = "home";

    
    public StorageController() {
        try {
            dao = StorageDAOFactory.getStorageDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
        } catch (DAOException ex) {
            logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
        }
    }

    public Storage getStorage() {
        return storage;
    }
    public void setStorage(Storage storage) {
        this.storage = storage;
    }
    
    
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Storage list paging functions">
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
        return listStorages();
    }

    public String prev() {
        firstItem -= batchSize;
        if (firstItem < 0) {
            firstItem = 0;
        }
        return listStorages();
    }

    public String listStorages() {
        storages = null;
        itemCount = -1;
        return "list_storages";
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="DAO methods">
    /* add new resource */
    public String prepareSolrStorageToAdd() {
        storage = new SolrStorage();
        return "new_solrstorage";
    }

    /* TODO Fix */
    public String prepareZebraStorageToAdd() {
        storage = new SolrStorage();
        return "new_zebrastorage";
    }

    public String prepareXmlStorageToAdd() {
        storage = new SolrStorage();
        return "new_xmlstorage";
    }

    public String prepareConsoleStorageToAdd() {
        storage = new SolrStorage();
        return "new_consolestorage";
    }

    
    public String addStorage() {
        prePersist();
        dao.createStorage(storage);
        storage = null;
        return listStorages();
    }

    /* update resource */
    public String prepareStorageToEdit() {
        storage = getResourceFromRequestParam();
        postDePersist();
        logger.log(Level.INFO, "Retrieved persisted resource of type " + storage.getClass().getName());
        if (storage instanceof SolrStorage) {
            return "edit_solrstorage";
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

    public String saveStorage() {
        prePersist();
        storage = dao.updateStorage(storage);
        storage = null;
        return listStorages();
    }

	private boolean isPb() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
    }

    /* list storages */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public DataModel getStorages() {
        //check if new request
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (storages == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
            req.setAttribute("listRequestSeen", "yes");
            storages = (List) dao.retrieveStorageBriefs(firstItem, batchSize);
        }
        if (storages != null)
            Collections.sort(storages);
        return new ListDataModel(storages);
    }

    public String deleteStorage() {
        storage = getResourceFromRequestParam();
        dao.deleteStorage(storage);
        storage = null;
        return listStorages();
    }
    
    public String saveAndPurge() {
    	
    	dao.deleteStorage(storage);
        prePersist();
        storage.setId(null);
        dao.createStorage(storage);
        storage = null;
        return listStorages();
    }
    
    private void prePersist() {        

    }

    private void postDePersist() {

    }

    public String viewStorageLog() {
    	
    	jobLog = "http://localhost:8080/solr/admin/";
        return "storage_log";

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
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("storageId");
                Long id = new Long(param);
                o = dao.retrieveStorageById(id);
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
	
	public List<SelectItem> getStorageItems() {
		List<SelectItem> list = new LinkedList<SelectItem>();
		if (storages == null) {
			/* TODO We need all (enabled) storages. Not just a window */
			getStorages();
		}
		list.add(new SelectItem("", "<Select Storage>"));
		if (storages != null)
			for (StorageBrief storage : (List<StorageBrief>) storages) {
				if (storage.isEnabled()) {
					SelectItem selectItem = new SelectItem();
					selectItem.setLabel(storage.getName());
					selectItem.setValue(storage.getId());
					list.add(selectItem);
				}
			}
    	return list;
    }


}
