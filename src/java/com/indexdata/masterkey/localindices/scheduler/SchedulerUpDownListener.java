/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Context listener for the scheduler app.
 * Starts the scheduling thread when the the app is deployed,
 * and kills it when the server is going down.
 * 
 * @author heikki
 */
public class SchedulerUpDownListener implements ServletContextListener {
    Thread th;
    SchedulerThread st;
    private String serviceBaseURL = "http://localhost:8080/harvester/resources/harvestables/";
    //private String serviceBaseURL = "http://localhost:8136/localindexes/resources/harvestables/";
    
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.err.println("ContextListener: harvester context is initialized...");
        ServletContext servletContext = servletContextEvent.getServletContext();
        st = new SchedulerThread(serviceBaseURL);
        th = new Thread(st);
        th.start();
        servletContext.setAttribute("schedulerThread", st);
        System.err.println("ContextListener: scheduling thread created, started and placed in the context...");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (st != null) {
            System.err.println("ContextListener: telling the scheduling thread to stop...");
            st.kill(); 
            System.err.println("ContextListener: waking the scheduling thread up so it can close down");
            th.interrupt();
        }
        System.err.println("ContextListener: application context destroyed.");
    } 



} // class MainScheduler

