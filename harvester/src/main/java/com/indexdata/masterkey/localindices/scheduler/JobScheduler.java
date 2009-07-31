/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorageFactory;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * JobScheduler schedules the job kept in the memory-based collection,
 * keeps them in touch with the persistent storage and reacts on status and error
 * changes.
 * @author jakub
 */
public class JobScheduler {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestableDAO dao;
    private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();
    private Map<String, Object> config;

    public JobScheduler(Map<String, Object> config) {
        dao = new HarvestablesDAOJPA();
        this.config = config;
    }

    /**
     * Update the current job list to reflect updates in the persistent storage.
     */
    public void updateJobs() {
        Collection<HarvestableBrief> hbriefs = dao.retrieveHarvestableBriefs(0, Integer.parseInt((String)config.get("harvester.max-jobs")));
        if (hbriefs == null) {
            logger.log(Level.ERROR, "Cannot update harvesting jobs, retrieved list is empty.");
        } else {
            // mark all job so we know what to remove
            for (JobInstance j : jobs.values()) {
                j.seen = false;
            }
            for (HarvestableBrief hbrief : hbriefs) {
                Long id = hbrief.getId();
                JobInstance ji = jobs.get(id);
                // corresponding job is in the current list
                if (ji != null) {
                    // and seetings has changed
                    if (!ji.getHarvestable().getLastUpdated().equals(hbrief.getLastUpdated())) {
                        logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId()
                                + " parameters changed (LU " + hbrief.getLastUpdated() + "), stopping thread and destroying job");
                        ji.stop();
                        ji = null; // signal to create a new one
                    // should we remove it from the list?
                    }
                    //but it's been disabled
                    if (!hbrief.isEnabled()) {
                        logger.log(Level.INFO, "JOB#" + id + " has been disabled");
                        if (ji != null) {
                            ji.stop();
                        }
                        jobs.remove(id);
                    }
                }
                // crate new job
                if (ji == null) {
                    if (!hbrief.isEnabled()) {
                        //logger.log(Level.INFO, "New JOB#" + hbrief.getId() + " is disabled, nothing will be created");
                    } else {
                        Harvestable harv = dao.retrieveFromBrief(hbrief);
                        try {
                            ji = new JobInstance(harv,
                                    HarvestStorageFactory.getStorage((String)config.get("harvester.dir"), harv)
                                    , (Proxy) config.get("harvester.http.proxy"));
                            jobs.put(id, ji);
                            logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " created.");
                        } catch (Exception e) {
                            logger.log(Level.ERROR, "Cannot update the current job list with " + harv.getId());
                            logger.log(Level.DEBUG, e);
                        }
                    }
                }
                if (ji != null) {
                    ji.seen = true;
                }
            }

            // kill jobs with no entities in the WS
            for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
                JobInstance ji = it.next();
                if (!ji.seen) {
                    logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " no longer in the DB. Deleting from list.");
                    //ji.stop();
                    ji.destroy();
                    it.remove();
                }
            }
        }
    }

    /**
     * Start, report status and error of the scheduled jobs.
     */
    public void checkJobs() {
        for (JobInstance ji : jobs.values()) {
            switch (ji.getStatus()) {
                case FINISHED: //update the lastHarvestStarted (and harvestedUntil) 
                    //and send received signal                    
                    ji.notifyFinish();
                    logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() 
                            + " has finished. Persisting state...");
                    dao.updateHarvestable(ji.getHarvestable());
                    //persist from and until
                    break;
                case ERROR:
                case NEW:     // ask if time to run
                case WAITING:
                    // should check harvested until?
                    if (ji.timeToRun()) {
                        ji.start();
                    }
                    break;
                case RUNNING: //do nothing (update progress bar?)
                    break;
                case KILLED: //zombie thread
                    break;
            }
            if (ji.statusChanged()) {
                logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() 
                        + " status updated to " + ji.getStatus());
                ji.getHarvestable().setCurrentStatus(ji.getStatus().name());
                dao.updateHarvestable(ji.getHarvestable());
            }
            if (ji.statusMsgChanged()) {
                logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() 
                        + " - status message updated - " + ji.getHarvestable().getMessage());
                dao.updateHarvestable(ji.getHarvestable());
            }
        }
    }

    /**
     * Return a collection of status and config information on the scheduled jobs.
     * @return collection of JobInfo objects
     */
    public Collection<JobInfo> getJobInfo() {
        Collection<JobInfo> jInfoList = new ArrayList<JobInfo>();
        for (JobInstance ji : jobs.values()) {
            JobInfo jInfo = new JobInfo();
            jInfo.setHarvestable(ji.getHarvestable());
            jInfo.setStatus(ji.getStatus());
            jInfo.setError(ji.getHarvestable().getMessage());
            jInfo.setHarvestPeriod("");
            jInfoList.add(jInfo);
        }
        return jInfoList;
    }

    /**
     * Brutally stop all jobs.
     */
    public void stopAllJobs() {
        for (JobInstance ji : jobs.values()) {
            ji.stop();
        }
    }

    /**
     * Stop the job with given id.
     * @param jobId
     */
    public void stopJob(Long jobId) {
        JobInstance ji = jobs.get(jobId);
        ji.stop();
    }
}
