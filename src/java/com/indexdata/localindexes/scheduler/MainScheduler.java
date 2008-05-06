
package com.indexdata.localindexes.scheduler;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// import com.indexdata.localindexes.scheduler.SchedulerThread;



/**
 * This is the main scheduler for the OAI harvester. 
 * Basically, it just starts a thread when the object is created, 
 * which should happen automagically when ever the app server is (re)started.
 * It also kills it when the server is going down.
 * 
 * @author heikki
 */


public class MainScheduler implements ServletContextListener {
    Thread th = null;
    SchedulerThread st = null;
    private String serviceBaseURL = "http://localhost:8080/localindexes/resources/harvestables/";
    //private String serviceBaseURL = "http://localhost:8136/localindexes/resources/harvestables/";

    public void contextDestroyed(ServletContextEvent arg0) {
        if (st != null) {
            System.err.println("MainScheduler: Telling the SchedulerThread to stop");
            st.kill(); 
            System.err.println("MainScheduler: Waking the SchedulerThread up so it can close down");
            th.interrupt();
        }
        System.err.println("MainScheduler: Destroyed");
    } 

    public void contextInitialized(ServletContextEvent arg0) {
        System.err.println("MainScheduler Context is initialized...");
        st = new SchedulerThread(serviceBaseURL);
        th = new Thread(st);
        th.start();
        System.err.println("Created and started the background thread...");

    }

} // class MainScheduler

