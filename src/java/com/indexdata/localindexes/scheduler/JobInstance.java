package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.web.entity.*;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import com.indexdata.masterkey.harvest.oai.HarvestJob;
import com.indexdata.masterkey.harvest.oai.HarvestStatus;
import com.indexdata.masterkey.harvest.oai.OAIHarvestJob;

/**
 * a JobInstance is one instance of a harvesting job.
 * It owns the actual harvesting thread, and has some status info,
 * as well as the parameters required to create the harvesting object.
 * 
 * @author heikki
 */
public class JobInstance {

    private Harvestable harvestableData;
    private Thread harvestingThread;
    private HarvestJob harvestJob;
    private CronLine cronLine;
    private HarvestStatus lastCheckedStatus;

    public boolean seen; // for checking what has been deleted


    public JobInstance(Harvestable hable) throws IllegalArgumentException {
        harvestableData = hable;

        if (hable instanceof OaiPmhResource) {
            harvestJob = new OAIHarvestJob((OaiPmhResource) hable);
        } else {
            throw new IllegalArgumentException("Cannot create instance of the harvester.");
        // TODO string in might not fit
        }
        cronLine = new CronLine(hable.getScheduleString());

        seen = false;
    } // JobInstance constructor


    public Harvestable getHarvestableData() {
        return harvestableData;
    }

    /**
     * kill the job instance
     *   Tell the thread to stop, if running
     *   Tell the harvesterThing to clean up
     */
    public void kill() {
        harvestJob.kill();
    } //


    public void startThread() {
        if (harvestingThread == null) {
            harvestingThread = new Thread(harvestJob);
            harvestingThread.start();
        }

    } // startThread


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
    public boolean isStatusChanged() {
        boolean changed;

        if (lastCheckedStatus == null) {
            changed = true;
        } else {
            changed = (lastCheckedStatus == harvestJob.getStatus());
        }
        lastCheckedStatus = harvestJob.getStatus();
        return changed;
    }
    
    public HarvestStatus getLastCheckedStatus() {
        return lastCheckedStatus;
    } //

} // class JobInstance
