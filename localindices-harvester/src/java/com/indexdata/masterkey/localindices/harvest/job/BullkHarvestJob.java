/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

/**
 *
 * @author jakub
 */
public class BullkHarvestJob implements HarvestJob {
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private XmlBulkResource resource;
    
    public BullkHarvestJob(XmlBulkResource resource) {
        this.resource = resource;
        
    }
    
    public void kill() {
        
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public HarvestStorage getStorage() {
        return storage;
    }

    public void finishReceived() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getError() {
        return error;
    }

    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
