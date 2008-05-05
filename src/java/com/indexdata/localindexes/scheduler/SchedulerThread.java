package com.indexdata.localindexes.scheduler;

import com.indexdata.masterkey.harvest.oai.HarvestStatus;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;



import com.indexdata.localindexes.web.entity.Harvestable;
// TODO : We need a logging thing too!
import com.indexdata.localindexes.web.entity.OaiPmhResource;
//import java.util.logging.Level;

import com.indexdata.localindexes.web.service.client.ResourceConnector;
import com.indexdata.localindexes.web.service.converter.HarvestableConverter;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import com.indexdata.localindexes.web.service.converter.HarvestablesConverter;
import java.net.MalformedURLException;
import java.net.URL;
//import java.util.logging.Logger;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SchedulerThread does the actual scheduling of harvester threads
 * Simple pseudocode:
 *    * Get a list of active jobs from the WS, and update the joblist
 *    * For each job in joblist
 *       * If time to run it (and not already running), start it
 *       * If running, poll for status, and pass on to the WS
 *    * Sleep a while
 *  
 * Missing:
 *    - use the HarvestableRef everywhere!
 *    - Some sort of logging
 *    - Poll the job list from Jakub's WS
 *    - Make use of Marc's harvesting classes
 *    - Create background threads
 *    - Get cron lines from the Harvestables
 *    - Compare current time to cron line
 * 
 * @author heikki
 */
public class SchedulerThread implements Runnable {
    private String serviceBaseURL = "http://localhost:8080/localindexes/resources/harvestables/";
    //private String serviceBaseURL = "http://localhost:8136/localindexes/resources/harvestables/";
    private boolean keeprunning = true;
    private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();

    private void reportStatus(JobInstance ji) {
        //Harvestable hStatus = new ji.getHarvestableData().getClass();
    }

    private synchronized boolean stillRunning() {
        return keeprunning;
    } //stillRunning

    public synchronized void enough() {
        System.err.print("Telling the thread to stop running");
        keeprunning = false;
    } // enough()

    public void run() {
        System.err.print("Starting to run the background thread...\n");
        keeprunning = true;
        int i = 0;
        testit();
        while (stillRunning()) {
            //System.err.print("Looping " + i + " \n");
            System.err.flush();
            i++;
            try {
                //mainLoop();
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                System.err.print("Caught an exception " + e.getMessage());
            // e.printStackTrace();
            }
        }
        System.err.print("Someone has told us to stop, exiting after " +
                i + " rounds\n");
    } // run()

    /**
     * The "main" loop:
     * If time to poll the WS
     *   Get a new job list from the WS
     *   Update the current joblist
     * Check if status changed in any running threads
     * Check if time to start new threads
     */
    private void mainLoop() {
        Collection<HarvestableRefConverter> reflist = pollJobRefList();
        updateJobs(reflist);
    } // mainLoop

    private Collection<HarvestableRefConverter> pollJobRefList() {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getReferences();
            //System.err.println("Got a list of " + hrefs.size() );
        } catch (Exception male) {
            Logger.getLogger("com.indexdata.localindexes.scheduler").log(Level.SEVERE, null, male);
            System.err.println("PollRefList: Exception: " + male.toString() );
        }
        return null;
    }

    private void updateJob(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    new URL(serviceBaseURL + harvestable.getId() + "/"),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestableConverter hc = new HarvestableConverter();
            hc.setEntity(harvestable);
            harvestableConnector.put(hc);
        } catch (Exception male) {
            Logger.getLogger("com.indexdata.localindexes.scheduler").log(Level.SEVERE, "", male);
        }
    } // updateJob

    private Harvestable retrieveFromRef(HarvestableRefConverter href) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            return harvestableConnector.get().getEntity();
        } catch (Exception male) {
            Logger.getLogger("com.indexdata.localindexes.scheduler").
                    log(Level.SEVERE, "", male);
        }
        return null;
    } // retrieveFromRef

    /**
     * Update the job list (jobs) based on a new list (refs)
     */
    private void updateJobs(Collection<HarvestableRefConverter> refs) {
        if (refs == null) {
            System.err.println("updatejobs called with null refs list");
        } else {
            for (JobInstance j : jobs.values()) {
                j.seen = false;
            }
            for (HarvestableRefConverter href : refs) {
                Long id = href.getId();
                if (id == null) {
                    System.err.println("Got a null id!!! "); 
                }
                System.err.println("Check-1 id=" + id.toString() );

                JobInstance ji = jobs.get(id);
                if (ji != null) {
                    if (ji.getHarvestable().getLastUpdated() != href.getLastUpdated()) {
                        System.err.println("Parameters changed for job " + ji + " killing old");
                        ji.killThread();
                        ji = null; // signal to creatre a new one
                    }
                }
                if (ji == null) {
                    Harvestable harv = retrieveFromRef(href);
                    try {
                        ji = new JobInstance(harv);
                        jobs.put(id, ji);
                    } catch (IllegalArgumentException ile) {
                       System.err.println(ile);
                    }
                    // todo: set status to 'starting' or something like that
                    System.err.println("Created a new JobInstance " + ji);
                }
                ji.seen = true;
            } // harvestables loop
            for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
                JobInstance ji = it.next();
                if (!ji.seen) {
                    System.err.println("Job " + ji.getHarvestable().getId() +
                            " gone missing. Deleting");
                    ji.killThread();
                    it.remove();
                }
            }
        }
    } // updateJobs
  

    /**
     * Check if the status has changed in any of the running threads
     * If so, pass on to the WS
     * If thread finished, remove the thread object 
     * (but keep the job for the next time)
     */
    private void checkThreads() {
        for(JobInstance ji : jobs.values()) {
            if(ji.errorChanged())
                reportStatus(ji);
        }
    } // checkThreads

    /**
     * Check all the jobs, and start threads for those whose time
     * has come.
     */
    private void checkTimes() {
        CronLine currentCronLine = CronLine.currentCronLine();
        for(JobInstance ji : jobs.values()) {
            if (ji.timeToRun(currentCronLine))
                ji.startThread();
        }
    } // startThreads

    /** * * * * * * * * * * * * * * * * * 
     * Some consistency checks
     * Remove these routines later!
     */

    private void testCronLine() {
        CronLine c = new CronLine("1 2 3 4 5");
        c = new CronLine("1 2 3 4");
        c = new CronLine("1  2 3 4  5");
        c = new CronLine("15,45 2/2 3 4 5");
        c = new CronLine("* * 17 3 *");
        CronLine cur = CronLine.currentCronLine();
        System.err.println("Current cronline is '" + cur.toString() + "'");
        System.err.println(c.toString() + " matches cur: " +
                cur.matches(c));
    }

    private void testUpdateJob() {
        Harvestable h = new OaiPmhResource();
        h.setId(new Long(100));
        h.setCurrentStatus("failed");
        h.setLastHarvestStarted(new Date());
        updateJob(h);
    }

    private void testMainLoop() {
        try {
            System.err.println("About to call mainloop for the first time");
            mainLoop();
            Thread.sleep(1000);
            System.err.println("About to call mainloop for the first time");
            mainLoop();
            Thread.sleep(10000);
            System.err.println("About to call mainloop for the first time");
            mainLoop();
            System.err.println("TestMainLoop done");
        } catch (InterruptedException ex) {
            //Logger.getLogger(SchedulerThread.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("testMainLoop interrupted while sleeping");
        }
    }

    private void testit() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        //Logger.getLogger(SchedulerThread.class.getName()).log(Level.SEVERE, null, ex);
        // just ignore it, this is a test routine to be deleted later
        }
        System.err.println("Testit starting * * * * * * * * * * * *");
        //testUpdateJobs();
        testMainLoop();
        testCronLine();
        testUpdateJob();
        System.err.println("Testit done * * * * * * * * * * * *");
    }
} // class SchedulerThread

