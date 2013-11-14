/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.SettingDAOFactory;
import com.indexdata.masterkey.localindices.entity.Setting;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author jakub
 */
@ManagedBean(name="settings")
@SessionScoped
public class SettingsController {
  Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  SettingDAO dao;

  public SettingsController() {
    try {
      dao = SettingDAOFactory.getDAO((ServletContext) FacesContext.getCurrentInstance()
	  .getExternalContext().getContext());
    } catch (DAOException ex) {
      logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
    }
  }
  
  /**
   * Lists available connector engine URLs.
   * @return 
   */
  public List<Setting> getConnectorEngines() {
    return dao.retrieve(0, 100, "cf.engine.url.", false);
  }
  
    /**
   * Lists available connector repo URLs.
   * @return 
   */
  public List<Setting> getConnectorRepos() {
    return dao.retrieve(0, 100, "cf.repo.url.", false);
  }
  
}
