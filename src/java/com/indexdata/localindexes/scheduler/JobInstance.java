package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.web.entity.*;
import com.indexdata.masterkey.harvest.oai.*;


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
    private String harvestError;
    
    public boolean seen; // for checking what has been deleted

    public JobInstance(Harvestable hable, HarvestStorage storage) throws IllegalArgumentException {
        // harvest job factory
        if (hable instanceof OaiPmhResource) {
            harvestJob = new OAIHarvestJob((OaiPmhResource) hable);
            harvestJob.setStorage(storage);
        } else {
            throw new IllegalArgumentException("Cannot create instance of the harvester.");
        }
        harvestable = hable;
        cronLine = new CronLine(hable.getScheduleString());
        seen = false;
    }

    public Harvestable getHarvestable() {
        return harvestable;
    }

    /**
     *   Tell the harvesting thread to stop, if running
     *   and the harvest job to clean up.
     */
    public void killThread() {
        harvestJob.kill();
    }

    public void startThread() {
        if (harvestingThread == null) {
            harvestingThread = new Thread(harvestJob);
            harvestingThread.start();
        }

    }

    /**
     * Checks if the time has come to run this job
     * @param curCl current time to check against
     * @return true/false
     */
    public boolean timeToRun(CronLine curCl) {
        if (harvestJob.getStatus().equals(HarvestStatus.WAITING) 
                || harvestJob.getStatus().equals(HarvestStatus.NEW)) {
            return curCl.matches(cronLine);
        }
        return false;
    }

    /**
     * Checks if the error status has changed since last check
     * @return true/false
     */
    public boolean errorChanged() {
        boolean changed;
        if (harvestError == null) {
            changed = true;
        } else {
            changed = harvestError.equals(harvestJob.getError());
        }
        harvestError = harvestJob.getError();
        return changed;
    }
    
    public String getError() {
        return harvestError;
    }

}
