/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.bean.SettingDAOJPA;
import com.indexdata.masterkey.localindices.entity.Setting;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//import com.indexdata.masterkey.localindices.harvest.storage.backend.StorageBackend;
//import com.indexdata.masterkey.localindices.harvest.storage.backend.ZebraStorageBackend;

/**
 * Context listener for the scheduler application. Starts the scheduling thread
 * when the the application is deployed, and kills it when the server is going
 * down. Places the schedulerThread in application context.
 * 
 * @author jakub
 */
public class SchedulerUpDownListener implements ServletContextListener {
  private final static String DEFAULT_PROPERTIES = "/WEB-INF/harvester.properties";
  private final static String USER_CONFIG_PATH_PARAM = "USER_CONFIG_PATH";
  private Thread th;
  private SchedulerThread st;
  private SettingDAO dao;
  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");

  public void contextInitialized(ServletContextEvent servletContextEvent) {
    logger.log(Level.INFO, "Harvester context is being initialized...");
    ServletContext ctx = servletContextEvent.getServletContext();
    //load default settings, packaged in the war file
    Properties props = new Properties();
    InputStream is = null;
    try {
        is = ctx.getResourceAsStream(DEFAULT_PROPERTIES);
        props.load(is);
    } catch (IOException ex) {
      logger.log(Level.FATAL,
	  "Cannot load '"+DEFAULT_PROPERTIES+"', harvester installation is unstable.");
    } finally {
      try {
        if (is != null) is.close();
      } catch (IOException ioe) {
        logger.fatal("Unexpected stream closing failure", ioe);
        is = null;
      }
    }
    //override with user configuration
    String configPath = ctx.getInitParameter(USER_CONFIG_PATH_PARAM);
    if (configPath == null) {
      logger.log(Level.WARN, "Init param " + USER_CONFIG_PATH_PARAM 
        + " not specified, will use defaults");
    } else {
      try {
	is = new FileInputStream(configPath);
	props.load(is);
      } catch (FileNotFoundException ex) {
        logger.warn("User config file '"+configPath+"' not found");
      } catch (IOException ioe) {
	logger.warn("Failed to load config from '"+configPath+"'", ioe);
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException ioe) {
          logger.fatal("Unexpected stream closing failure", ioe);
          is = null;
        }
      }
    }
    //read DB settings with prefix 'harvester.'
    logger.debug("Attempting to read 'harvester.' settings from the DB...");
    dao = new SettingDAOJPA();
    String prefix = "harvester.";
    List<Setting> settings = dao.retrieve(0, dao.getCount(prefix), prefix, false);
    for (Setting setting : settings) {
      logger.debug("Setting "+setting.getName()+"="+setting.getValue());
      props.put(setting.getName(), setting.getValue());
    }
    //store raw props in the context
    ctx.setAttribute("harvester.properties", props);
    //prepare typed configuration
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Map<String, Object> config = new HashMap(props);
    // http proxy settings
    String proxyHost = props.getProperty("harvester.http.proxyHost");
    if (proxyHost != null) {
      int proxyPort = 80;
      try {
	proxyPort = Integer.parseInt(props.getProperty("harvester.http.proxyPort"));
      } catch (NumberFormatException nfe) {
	logger.log(Level.WARN, "Http proxy port is invalid");
      }
      Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost,
	  proxyPort));
      config.put("harvester.http.proxy", proxy);
    }
    // put the config and scheduler in the context
    ctx.setAttribute("harvester.config", config);
    st = new SchedulerThread(config, props);
    th = new Thread(st, "SchedulerThread");
    th.start();
    ctx.setAttribute("schedulerThread", st);
    logger.log(Level.INFO, "Scheduler created, started and placed in the context.");
  }

  public final static ServletContext getContext() {
    return null;
  };

  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    logger.log(Level.INFO, "Harvester is being undeployed...");
    if (st != null) {
      logger.log(Level.INFO, "Stopping the scheduler...");
      st.kill();
      logger.log(Level.INFO, "Interrupting the scheduler...");
      th.interrupt();
    }
    logger.log(Level.INFO, "Harvester context destroyed.");
  }

  @SuppressWarnings("unused")
  private Map<String, String> getInitParamsAsMap(ServletContext ctx) {
    Map<String, String> paramMap = new HashMap<String, String>();
    @SuppressWarnings("rawtypes")
    Enumeration paramNames = ctx.getInitParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = (String) paramNames.nextElement();
      paramMap.put(paramName, ctx.getInitParameter(paramName));
    }
    return paramMap;
  }
}
