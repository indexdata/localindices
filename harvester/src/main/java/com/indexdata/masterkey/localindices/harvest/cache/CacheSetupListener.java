/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.cache;

import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author jakub
 */
public class CacheSetupListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    //we really need a DI library
    Properties props = (Properties) sce.getServletContext().getAttribute("harvester.properties");
    String cacheDir = props.getProperty("harvester.cache.dir");
    DiskCache.setBasePath(cacheDir);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    
  }
  
}
