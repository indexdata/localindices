/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.entity.*;
import com.indexdata.masterkey.localindices.harvest.job.*;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a JobInstance is one instance of a harvesting job.
 * It owns the actual harvesting thread, and has some status info,
 * as well as the parameters required to create the harvesting object.
 * 
 * @author heikki
 */
public class JobInstance {

    private Harvestable harvestable;
    private Thread harvestingThread;
    private HarvestJob harvestJob;
    private CronLine cronLine;
    private CronLine lastCronLine;
    private String lastHarvestError;
    private HarvestStatus lastHarvestStatus;
    private Date lastHarvestStarted;

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindexes.scheduler");
    
    public boolean seen; // for checking what has been deleted
    public JobInstance(Harvestable hable, HarvestStorage storage) throws IllegalArgumentException {
        // harvest job factory
        cronLine = new CronLine(hable.getScheduleString());
        if (hable instanceof OaiPmhResource) {
            if (cronLine.shortestPeriod() < CronLine.DAILY_PERIOD) {
                Calendar cal = Calendar.getInstance();
                int min = cal.get(Calendar.MINUTE);
                int hr = cal.get(Calendar.HOUR_OF_DAY);
                cronLine = new CronLine(min + " " + hr + " " + "* * *");
                logger.log(Level.WARNING, 
                        "Job scheduled with lower than daily granularity. Schedule overrriden to " + cronLine);
            }
            harvestJob = new OAIHarvestJob((OaiPmhResource) hable);
            harvestJob.setStorage(storage);
        } else {
            throw new IllegalArgumentException("Cannot create instance of the harvester.");
        }
        harvestable = hable;
        if (hable.getCurrentStatus() != null)
            lastHarvestStatus = HarvestStatus.valueOf(hable.getCurrentStatus());
        lastHarvestError = hable.getError();
        seen = false;
    }

    public Harvestable getHarvestable() {
        return harvestable;
    }

    /**
     * Tell the harvesting thread to stop, the harvesting thread 
     * may or may not clean-up the harvested data.
     */
    public void killThread() {
        harvestJob.kill();
    }
    public void purge() {
        harvestJob.kill();
        try {
            harvestJob.getStorage().purge();
        } catch (IOException ex) {
            Logger.getLogger(JobInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Start the harvesting thread for this job.
     */
    public void startThread() {
        if (harvestingThread == null || !harvestingThread.isAlive()) {
            harvestingThread = new Thread(harvestJob);
            harvestingThread.start();
            lastHarvestStarted = new Date();
        }
    }
    
    public void setStatusToWaiting() {
        harvestJob.finishReceived();
    }

    /**
     * Checks if the time has come to run the harvesting thread.
     * @return true/false
     */
    public boolean timeToRun() {
        CronLine curCron = CronLine.currentCronLine();
        if ( (lastCronLine != null ) && lastCronLine.matches(curCron))
            return false;
        lastCronLine=curCron;
        return curCron.matches(cronLine);
    }

    /**
     * Checks if the harvesting job error has changed since the last check.
     * @return true/false
     */
    public boolean errorChanged() {
        boolean changed;
        if (lastHarvestError == null) {
            changed = true;
        } else {
            changed = !lastHarvestError.equals(harvestJob.getError());
        }
        lastHarvestError = harvestJob.getError();
        return changed;
    }
    
    /**
     * Return last harvesting job error.
     * @return harvesting job error
     */
    public String getError() {
        return lastHarvestError;
    }
    
    public boolean statusChanged() {
        boolean changed;
        if (lastHarvestStatus == null) {
            changed = true;
        } else {
            changed = !(lastHarvestStatus == harvestJob.getStatus());
        }
        lastHarvestStatus = harvestJob.getStatus();
        return changed; 
    }
    /**
     * Returns harvesting job status.
     * @return harvesting job status
     */
    public HarvestStatus getStatus() {
        return harvestJob.getStatus();
    }

    public Date getLastHarvestStarted() {
        return lastHarvestStarted;
    }
}
