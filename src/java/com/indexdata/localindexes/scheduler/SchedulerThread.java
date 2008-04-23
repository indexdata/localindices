package com.indexdata.localindexes.scheduler;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;



import com.indexdata.localindexes.web.entity.Harvestable;
// TODO : We need a logging thing too!
import com.indexdata.localindexes.web.entity.OaiPmhResource;
//import java.util.logging.Level;
import com.indexdata.localindexes.web.service.HarvestablesResource;
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
import sun.rmi.runtime.Log;

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
    private boolean keeprunning = true;
    private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();

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
            System.err.print("Looping " + i + " \n");
            System.err.flush();
            i++;
            try {
                mainLoop();
                Thread.sleep(5000);
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
        
    } // mainLoop

    private Collection<Harvestable> pollJobList() {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            
            // TODO try to optimize polling
            Collection<Harvestable> harvestables = new ArrayList<Harvestable>();
            HarvestablesConverter hc = harvestablesConnector.get();
            for (HarvestableRefConverter ref : hc.getReferences()) {
                ResourceConnector<HarvestableConverter> harvestableConnector =
                        new ResourceConnector<HarvestableConverter>(
                        ref.getResourceUri().toURL(),
                        "com.indexdata.localindexes.web.entity" +
                        ":com.indexdata.localindexes.web.service.converter");
                 harvestables.add(harvestableConnector.get().getEntity());
            }
            return harvestables;
        } catch (Exception male) {
            Logger.getLogger("com.indexdata.localindexes.scheduler").log(Level.SEVERE, null, male);
        }
        return null;        
    } // pollJobList()
    
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
    }
    
    /**
     * Update the job list (jobs) based on a new list (newjoblist)
     *   Clear seen-marks on all jobs
     *   For each job in newjoblist
     *     If found in jobs, or its parameters changed since last look
     *       Delete it
     *     If not found in jobs (or deleted above)
     *       create a JobInstance for it
     *     Mark as seen
     *   For each job in jobs
     *     If not seen
     *       Kill the harvesting thread (if running)
     *       Delete the job
     */
    private void updateJobs(Collection<Harvestable> harvestables) {
        for ( JobInstance j : jobs.values() )
            j.seen = false;
        for ( Harvestable harv : harvestables ) {
            Long id = harv.getId();
            JobInstance ji = jobs.get(id);
            if (ji != null ) {
                if ( ji.harvestableData.getLastUpdated() != harv.getLastUpdated() ) {
                    System.err.println("Parameters changed for job " + ji +" killing old");
                    ji.kill();
                    ji = null; // signal to create a new one
                }                    
            }
            if (ji == null ) {
                ji = new JobInstance( harv );              
                jobs.put(id, ji);
                System.err.println("Created a new JobInstance " + ji);
            } 
            ji.seen = true;
        } // harvestables loop
        for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
            JobInstance ji = it.next();
            if (!ji.seen) {
                System.err.println("Job " + ji.harvestableData.getId() + 
                        " gone missing. Deleting" );
                ji.kill();
                it.remove();
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
    } // checkThreads
    
    /**
     * Check all the jobs, and start threads for those whose time
     * has come.
     */
    private void checkTimes() {
    } // startThreads

    
    /** * * * * * * * * * * * * * * * * * 
     * Some consistency checks
     * Remove these routines later!
     */
    
    private void testUpdateJobs() {
        Collection<Harvestable> hlist = new ArrayList<Harvestable>();
        Harvestable h1 = new OaiPmhResource();
        h1.setId(new Long(1));
        h1.setTitle("h1");
        h1.setScheduleString("* *  * * *"); 
        hlist.add(h1);

        Harvestable h2 = new OaiPmhResource();
        h2.setId(new Long(2));
        h2.setTitle("h2");
        hlist.add(h2);

        System.err.println("First list of Harvestables: " + hlist.toString() );
        this.updateJobs(hlist);
        System.err.println("First joblist: " + this.jobs.toString() );

        Harvestable h3 = new OaiPmhResource();
        h3.setId(new Long(3));
        h3.setTitle("h3");
        hlist.add(h3);

        hlist.remove(h2);
        
        hlist = pollJobList();        
        this.updateJobs(hlist);
        System.err.println("Second joblist: " + this.jobs.toString() );
        
    } // tetUpdateJobs
    
    private void testCronLine() {
        CronLine c = new CronLine("1 2 3 4 5");
        c = new CronLine("1 2 3 4");
        c = new CronLine("1  2 3 4  5");
        c = new CronLine("15,45 2/2 3 4 5");
        c = new CronLine("* * 17 3 *");
        CronLine cur = CronLine.currentCronLine();
        System.err.println("Current cronline is '" + cur.toString()+"'" );
        System.err.println( c.toString() + " matches cur: " + 
                cur.matches(cur) );
    }
    
    private void testUpdateJob () {
        Harvestable h = new OaiPmhResource();
        h.setId(new Long(100));
        h.setCurrentStatus("failed");
        h.setLastHarvestStarted(new Date());
        updateJob(h);
    }
    
    private void testit() {
        try {
            Thread.sleep(5000); 
        } catch (InterruptedException ex) {
            //Logger.getLogger(SchedulerThread.class.getName()).log(Level.SEVERE, null, ex);
            // just ignore it, this is a test routine to be deleted later
        }
        System.err.println("Testit starting * * * * * * * * * * * *");
        testUpdateJobs(); 
        testCronLine();
        testUpdateJob();
        System.err.println("Testit done * * * * * * * * * * * *");
    }
    
} // class SchedulerThread

