/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
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

  private Thread th;
  private SchedulerThread st;
  /*
   * private StorageBackend storageBackend; private StorageBackend
   * reindexStorageBackend;
   */
  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");

  public void contextInitialized(ServletContextEvent servletContextEvent) {

    logger.log(Level.INFO, "Harvester context is being initialized...");
    ServletContext ctx = servletContextEvent.getServletContext();
    // load default, fallback settings
    Properties props = null;
    try {
      props = loadDefaults(ctx);
    } catch (IOException ex) {
      logger.log(Level.FATAL,
	  "Default harvester properties missing from the archive, deployment aborted.");
      return;
    }
    // override with user settings, if any otherwise try to unpack the defaults
    String configPath = ctx.getInitParameter("USER_CONFIG_PATH");
    if (configPath == null) {
      logger.log(Level.WARN, "CONFIG_PATH not specified, will use only defaults");
    } else {
      try {
	FileInputStream userPropsFis = new FileInputStream(configPath);
	props.load(userPropsFis);
	userPropsFis.close();
      } catch (FileNotFoundException ex) {
	FileOutputStream fos;
	try {
	  fos = new FileOutputStream(configPath);
	  props.store(fos,
	      "In order for the canges to cause effect, the harvester needs to be redeployed");
	  fos.close();
	} catch (FileNotFoundException ex2) {
	  // very weird
	  logger.log(Level.WARN, ex2);
	} catch (IOException ioe) {
	  logger.log(Level.WARN, ioe);
	}
      } catch (IOException ioe) {
	logger.log(Level.WARN, ioe);
      }
      // TODO Updated Any RUNNING states to INTERRUPTED 
      // We cannot just restarts the jobs. 
    }

    /* Refactor for multiple backends and types of backends */
    /*
     * Disable ZebraStorage for now
     * 
     * String harvestDirPath = props.getProperty("harvester.dir");
     * storageBackend = new ZebraStorageBackend(harvestDirPath, "idx");
     * storageBackend.init(props); storageBackend.start();
     */
    /* TODO: re-index should be hidden behind the StorageBackend */
    /*
     * reindexStorageBackend = new ZebraStorageBackend(harvestDirPath, "reidx");
     * reindexStorageBackend.init(props);
     */
    // load properties to a config
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
    st = new SchedulerThread(config);
    th = new Thread(st);
    th.start();
    ctx.setAttribute("schedulerThread", st);
    logger.log(Level.INFO, "Scheduler created, started and placed in the context.");
  }

  public ServletContext getContext() {
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
    /*
     * if (storageBackend != null) { logger.log(Level.INFO,
     * "Shutting down Storage Backend..."); storageBackend.stop(); } if
     * (reindexStorageBackend != null) { logger.log(Level.INFO,
     * "Shutting down Storage Backend..."); reindexStorageBackend.stop(); }
     */
    logger.log(Level.INFO, "Harvester context destroyed.");
  }

  private Properties loadDefaults(ServletContext ctx) throws IOException {
    InputStream is = ctx.getResourceAsStream("/WEB-INF/harvester.properties");
    Properties props = new Properties();
    props.load(is);
    return props;
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

  /*
   * Another idea.... Implement some callback. Code from re-index InitCallback
   * reindexCallback = new InitCallback() { private ServletContext ctx; {
   * this.ctx = getContext();
   * 
   * } private String harvestDirPath; public void
   * setServletContext(ServletContext ctx) { this.ctx = ctx; };
   * 
   * public void init(StorageBackend backend) { String[] tokensRe =
   * {"CONFIG_DIR", harvestDirPath, "HARVEST_DIR", harvestDirPath + "/reidx"};
   * unpackResourceWithSubstitute(ctx, "/WEB-INF/zebra.cfg", harvestDirPath +
   * "/zebra-reidx.cfg", tokensRe); unpackResourceWithSubstitute(ctx,
   * "/WEB-INF/reindex.rb", harvestDirPath + "/reindex.rb", null);
   * unpackResourceWithSubstitute(ctx, "/WEB-INF/addlexis.rb", harvestDirPath +
   * "/addlexis.rb", null); } };
   */
}
