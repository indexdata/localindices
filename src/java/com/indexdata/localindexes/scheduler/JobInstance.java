package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.web.entity.*;
import com.indexdata.masterkey.harvest.oai.*;
import java.util.Date;

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
     * Tell the harvesting thread to stop, the harvesting thread 
     * may or may not clean-up the harvested data.
     */
    public void killThread() {
        harvestJob.kill();
    }

    /**
     * Start the harvesting thread for this job.
     */
    public void startThread() {
        if (harvestingThread == null || !harvestingThread.isAlive()) {
            harvestingThread = new Thread(harvestJob);
            harvestingThread.start();
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
        return CronLine.currentCronLine().matches(cronLine);
    }

    /**
     * Checks if the harvesting job error has changed since the last check.
     * @return true/false
     */
    public boolean errorChanged() {
        boolean changed;
        if (harvestError == null) {
            changed = false;
        } else {
            changed = !harvestError.equals(harvestJob.getError());
        }
        harvestError = harvestJob.getError();
        return changed;
    }
    
    /**
     * Return last harvesting job error.
     * @return harvesting job error
     */
    public String getError() {
        return harvestError;
    }
    
    /**
     * Returns harvesting job status.
     * @return harvesting job status
     */
    public HarvestStatus getStatus() {
        return harvestJob.getStatus();
    }

}
