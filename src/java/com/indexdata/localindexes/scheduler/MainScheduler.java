/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.localindexes.scheduler;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This is the main scheduler for the OAI harvester. 
 * Basically, it just starts a thread when the object is created, 
 * which should happen automagically when ever the app server is (re)started.
 * It also kills it when the server is going down.
 * 
 * @author heikki
 */
public class MainScheduler implements ServletContextListener {
    Thread th;
    SchedulerThread st;
    private String serviceBaseURL = "http://localhost:8080/localindexes/resources/harvestables/";
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

