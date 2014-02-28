/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.SettingDAOFactory;
import com.indexdata.masterkey.localindices.entity.Setting;

/**
 *
 * @author jakub
 */
@ManagedBean(name="settings")
@SessionScoped
public class SettingsController {
  Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  SettingDAO dao;
  Setting setting = new Setting();
  private List<Setting> settings;

  public SettingsController() {
    try {
      dao = SettingDAOFactory.getDAO((ServletContext) FacesContext.getCurrentInstance()
	  .getExternalContext().getContext());
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }
  
  public void initialize() {
    logger.info("Retrieving settings from the harvester...");
    settings = dao.retrieve(0, 100);
  }
  
  /**
   * Lists available connector engine URLs.
   * @return 
   */
  public List<Setting> getConnectorEngines() {
    return dao.retrieve(0, 100, "cf.engine.url.", false);
  }
  
  public List<Setting> getSettings() {
    return settings;
  }
  
  
    /**
   * Lists available connector repo URLs.
   * @return 
   */
  public List<Setting> getConnectorRepos() {
    return dao.retrieve(0, 100, "cf.repo.url.", false);
  }

  public Setting getSetting() {
    return setting;
  }

  public void setSetting(Setting setting) {
    this.setting = setting;
  }
  
  //ui methods
  
  public String save(Setting s) {
    dao.update(s);
    return null; //same page
  }
  
  public String add() {
    dao.create(setting);
    setting = new Setting();
    return null;
  }
  
  public String delete(Setting s) {
    try {
      dao.delete(s);
    } catch (EntityInUse eiu) {
      return "failure";
    }
    return null;
  }
  
  public String list() {
    return "settings";
  }
  
}
