/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Context listener for the scheduler application.
 * Starts the scheduling thread when the the application is deployed,
 * and kills it when the server is going down. Places the schedulerThread 
 * in application context.
 * 
 * @author heikki
 */
public class SchedulerUpDownListener implements ServletContextListener {
    private Thread th;
    private SchedulerThread st;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");
    
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.log(Level.INFO, "SchedulerUpDownListener: harvester context is initialized...");
        ServletContext servletContext = servletContextEvent.getServletContext();
        st = new SchedulerThread();
        th = new Thread(st);
        th.start();
        servletContext.setAttribute("schedulerThread", st);
        logger.log(Level.INFO, "SchedulerUpDownListener: scheduling thread created, started and placed in the context.");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (st != null) {
            logger.log(Level.INFO, "SchedulerUpDownListener: telling the scheduling thread to stop...");
            st.kill(); 
            logger.log(Level.INFO, "SchedulerUpDownListener: waking the scheduling thread up so it can close down...");
            th.interrupt();
        }
        logger.log(Level.INFO, "SchedulerUpDownListener: application context destroyed.");
    } 
}

