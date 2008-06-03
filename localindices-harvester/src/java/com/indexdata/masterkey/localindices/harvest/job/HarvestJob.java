/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

/**
 * This interface a runnable harvest job controlled by the JobInstance
 * 
 * @author jakub
 */
public interface HarvestJob extends Runnable {
    
    /** 
     * Stop the current job and rollback the current harvest, 
     * deleting files received so far. Does not touch the older harvests.
     */
    public void kill();

    /**
     * Get latest harvest status.
     * 
     * @return current status
     */
    public HarvestStatus getStatus();
    
    /**
     * Sets the storage for the the harvested data.
     * @param storage for the harvest
     */
    public void setStorage(HarvestStorage storage);
    
    /**
     * Returns storage currently used for harvested data.
     * @return current storage
     */
    public HarvestStorage getStorage();
    
    /**
     * Inform the harvestesting job the the files harvest were received.
     */
    public void finishReceived();
    
    /**
     * Get last harvesting error.
     * @return
     */
    public String getError();
    
}
