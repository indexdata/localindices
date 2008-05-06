/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.scheduler.dao.HarvestableDAO;
import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import com.indexdata.masterkey.harvest.oai.HarvestStorage;
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
            logger.log(Level.SEVERE, "harvestableRef list is null. Cannot update current job list");
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
                    if (ji.getHarvestable().getLastUpdated() != href.getLastUpdated()) {
                        logger.log(Level.INFO, "Parameters changed for job " + ji + ", killing old thread.");
                        ji.killThread();
                        ji = null; // signal to create a new one
                        // should we remove it from the list?
                    }
                }
                // crate new job
                if (ji == null) {
                    Harvestable harv = dao.retrieveFromRef(href);
                    try {
                        ji = new JobInstance(harv, storage);
                        jobs.put(id, ji);
                        logger.log(Level.INFO, "Created a new job with id: " + ji.getHarvestable().getId());
                    } catch (IllegalArgumentException ile) {
                        logger.log(Level.SEVERE, "Cannot update the current job list with given entity.", ile);
                    }
                }
                ji.seen = true;
            }

            // kill jobs with no entities in the WS
            for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
                JobInstance ji = it.next();
                if (!ji.seen) {
                    logger.log(Level.INFO, "Job " + ji.getHarvestable().getId() +
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
        CronLine currentCronLine = CronLine.currentCronLine();
        for (JobInstance ji : jobs.values()) {
            if (ji.timeToRun(currentCronLine)) {
                ji.startThread();
            }
            if (ji.errorChanged()) {
                reportJobStatus(ji);
            }
        }
    }
    
    /**
     * Reports job status back to the Web Service
     * @param ji running job instance
     */
    private void reportJobStatus(JobInstance ji) {
        // this gotta report back to the WS
        logger.log(Level.INFO, "Harvesting job error has changed to: " + ji.getError());
    }
}
