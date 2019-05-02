/*
 * Copyright (c) 1995-2011, Index Data
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
import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAOFactory;
import com.indexdata.masterkey.localindices.entity.FileStorageEntity;
import com.indexdata.masterkey.localindices.entity.InventoryStorageEntity;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;

/**
 * The controller for the Admin interface for storages, implements all the
 * business logic and controls data access through DAO object
 *
 * @author Dennis
 */
public class StorageController {
  private Logger logger = Logger.getLogger(getClass());
  // Storage
  private StorageDAO dao;
  private Storage storage;

  @SuppressWarnings("rawtypes")
  private DataModel model;
  private List<StorageBrief> storages;
  private String storageAdminSite;
  Stack<String> backActions = new Stack<String>();
  String homeAction = "home";
  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Storage list paging functions">
  private int firstItem = 0;
  private int batchSize = 20;
  private int itemCount = -1;


  public StorageController() {
    try {
      dao = StorageDAOFactory.getStorageDAO((ServletContext) FacesContext.getCurrentInstance()
	  .getExternalContext().getContext());
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

  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="DAO methods">
  /* add new resource */
  public String prepareSolrStorageToAdd() {
    storage = new SolrStorageEntity();
    String customClass = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("customClass");
    if (customClass != null) {
      storage.setCustomClass(customClass);
    }
    return "new_solrstorage";
  }

  /* TODO Fix */
  public String prepareZebraStorageToAdd() {
    storage = new SolrStorageEntity();
    return "new_zebrastorage";
  }

  public String prepareXmlStorageToAdd() {
    storage = new SolrStorageEntity();
    return "new_xmlstorage";
  }

  public String prepareConsoleStorageToAdd() {
    storage = new FileStorageEntity();
    return "new_consolestorage";
  }

  public String prepareInventoryStorageToAdd() {
    logger.log(Level.INFO, "Invoked prepareInventoryStorageToAdd()");
    storage = new InventoryStorageEntity();
    return "new_inventorystorage";
  }

  public String addStorage() {
    prePersist();
    dao.create(storage);
    storage = null;
    return listStorages();
  }

  /* update resource */
  public String prepareStorageToEdit() {
    storage = getResourceFromRequestParam();
    postDePersist();
    logger.log(Level.INFO, "Retrieved persisted resource of type " + storage.getClass().getName());
    if (storage instanceof SolrStorageEntity) {
      return "edit_solrstorage";
    } else if (storage instanceof InventoryStorageEntity) {
      return "edit_inventorystorage";
    }
    /*
     * else if (resource instanceof ZebraStorage) { return "edit_zebrastorage";
     * } else if (resource instanceof ConsoleStorage) { return "edit_console"; }
     * else if (resource instanceof XmlStorage) { return "edit_xmlstorage"; }
     */
    else {
      logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
      return "failure";
    }
  }

  public String saveStorage() {
    prePersist();
    storage = dao.update(storage);
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
    // check if new request
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
	.getExternalContext().getRequest();
    if (storages == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
      req.setAttribute("listRequestSeen", "yes");
      storages = (List) dao.retrieveBriefs(firstItem, batchSize);
    }
    if (storages != null)
      Collections.sort(storages);
    return new ListDataModel(storages);
  }

  public String deleteStorage() {
    storage = getResourceFromRequestParam();
    try {
      dao.delete(storage);
    } catch (EntityInUse ex) {
      logger.warn("Cannot remove storage that is in use!", ex);
      return "failure";
    }
    storage = null;
    return listStorages();
  }

  public String saveAndPurge() {
    try {
      dao.delete(storage);
    } catch (EntityInUse ex) {
      logger.warn("Cannot remove storage that is in use!", ex);
    }
    prePersist();
    storage.setId(null);
    dao.create(storage);
    storage = null;
    return listStorages();
  }

  private void prePersist() {

  }

  private void postDePersist() {

  }

  @SuppressWarnings("unused")
  private String getStorageAdmin() {
    Storage storage = getResourceFromRequestParam();
    if (storageAdminSite == null)
    if (storage instanceof SolrStorageEntity) {
      Storage solrStorage = (Storage) storage;
      storageAdminSite = solrStorage.getSearchUrl() + "admin";
      return storageAdminSite;
    }
    return "";
  }

  public void setStorageAdmin(String adminSite) {
    storageAdminSite = adminSite;
  }

  // </editor-fold>

  /* objects from request */
  public Storage getResourceFromRequestParam() {
    Storage o = null;
    if (model != null) {
      o = (Storage) model.getRowData();
      // o = em.merge(o);
    } else {
      String param = FacesContext.getCurrentInstance().getExternalContext()
	  .getRequestParameterMap().get("storageId");
      if (param != null) {
	logger.log(Level.DEBUG, "StorageId found " + param);
	Long id = new Long(param);
	o = dao.retrieveById(id);
      }
      else
	logger.log(Level.ERROR, "No StorageId parameter found on request");
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
    List<StorageBrief> storages = dao.retrieveBriefs(0, dao.getCount());
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
