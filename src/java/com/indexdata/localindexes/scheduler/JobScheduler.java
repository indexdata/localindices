/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.scheduler.dao.HarvestableDAO;
import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import com.indexdata.masterkey.harvest.oai.FileStorage;
import com.indexdata.masterkey.harvest.oai.HarvestStatus;
import com.indexdata.masterkey.harvest.oai.HarvestStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
public class JobScheduler {
    private HarvestableDAO dao;
    private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();
    private static Logger logger;
    private HarvestStorage storage;

    public JobScheduler(HarvestableDAO dao, HarvestStorage storage, Logger aLogger) {
        this.dao = dao;
        this.storage = storage;
        logger = aLogger;
    }
    
    /**
     * Update the current job list (jobs) based on a polled harvestableRef list (refs)
     */
    public void updateJobs() {
        Collection<HarvestableRefConverter> refs = dao.pollHarvestableRefList();
        if (refs == null) {
            logger.log(Level.SEVERE, Thread.currentThread().getName() + ": harvestableRef list is null. Cannot update current job list");
        } else {
            // mark all job so we know what to remove
            for (JobInstance j : jobs.values()) {
                j.seen = false;
            }
            for (HarvestableRefConverter href : refs) {
                Long id = href.getId();
                JobInstance ji = jobs.get(id);
                // corresponding job is in the current list
                if (ji != null) {
                    // and seetings has changed
                    logger.log(Level.INFO, "job's lu: " + ji.getHarvestable().getLastUpdated()
                            + "retrieved lu: " + href.getLastUpdated() );
                    if (!ji.getHarvestable().getLastUpdated().equals(href.getLastUpdated())) {
                        logger.log(Level.INFO, Thread.currentThread().getName() + ": Parameters changed for job with id: " 
                                + ji.getHarvestable().getId() + ", killing old harvesting thread.");
                        ji.killThread();
                        ji = null; // signal to create a new one
                        // should we remove it from the list?
                    }
                }
                // crate new job
                if (ji == null) {
                    Harvestable harv = dao.retrieveFromRef(href);
                    try {
                        ji = new JobInstance(harv, new FileStorage(harv));
                        jobs.put(id, ji);
                        logger.log(Level.INFO, Thread.currentThread().getName() + ": Created a new job with id: " + ji.getHarvestable().getId());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, Thread.currentThread().getName() + ": Cannot update the current job list with given entity.", e);
                    }
                }
                ji.seen = true;
            }

            // kill jobs with no entities in the WS
            for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
                JobInstance ji = it.next();
                if (!ji.seen) {
                    logger.log(Level.INFO, Thread.currentThread().getName() + ": Job with id: " + ji.getHarvestable().getId() +
                            " gone missing. Deleting");
                    ji.killThread();
                    it.remove();
                }
            }
        }
    } // updateJobs
    
    /**
     * Start, kill, report status of the running jobs.
     */
    public void checkJobs() {
        for (JobInstance ji : jobs.values()) {
            switch(ji.getStatus()) {
                case FINISHED: //update the lastHarvestStarted (harvestedUntil) and send received signal
                    ji.setStatusToWaiting();
                    break;
                case ERROR:   // report error if changed
                    if (ji.errorChanged()) reportError(ji.getHarvestable(), ji.getError());
                    break;
                case NEW:     // ask if time to run
                case WAITING:
                    if (ji.timeToRun()) ji.startThread();
                    break;
                case RUNNING: //do nothing (update progress bar)
                    break;
                case KILLED: //never happens
                    break;
            }
        }
    }
    
    public Collection<JobInfo> getJobInfo() {
        Collection<JobInfo> jInfoList = new ArrayList<JobInfo>();
        for (JobInstance ji : jobs.values()) {
            JobInfo jInfo = new JobInfo();
            jInfo.setHarvestable(ji.getHarvestable());
            jInfo.setStatus(ji.getStatus());
            jInfoList.add(jInfo);
        }
        return jInfoList;
    }
    
    public void stopAllJobs() {
        for (JobInstance ji : jobs.values()) {
            ji.killThread();
        }
    }
    
    public void stopJob(Long jobId) {
        JobInstance ji = jobs.get(jobId);
        ji.killThread();
    }
    
    /**
     * Reports job status back to the Web Service
     * @param ji running job instance
     */
    private void reportError(Harvestable hable, String error) {
        // this gotta report back to the WS
        logger.log(Level.INFO, "Job with id " + hable.getId() + " has changed error to " + error);
    }
    
    private void reportStatus(Harvestable hable, HarvestStatus status) {
        logger.log(Level.SEVERE, "Job with id " + hable.getId() + " has changed status to " + status);
    }
}
