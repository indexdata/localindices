/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * @author jakub
 */
public abstract class AbstractRecordHarvestJob implements RecordHarvestJob {
    private boolean updated;
    private RecordStorage storage;
    private HarvestStatus status;
    private boolean die;

    protected final void setStatus(HarvestStatus status) {
        this.status = status;
    }

    protected final void markForUpdate() {
        updated = true;
    }

    protected synchronized boolean isKillSent() {
        return die;
    }

    @Override
    public final synchronized void kill() {
        die = true;
    }

    @Override
    public final HarvestStatus getStatus() {
        return status;
    }

    @Override
    public final void setStorage(RecordStorage storage) {
        this.storage = storage;
    }

    @Override
    public final RecordStorage getStorage() {
        return this.storage;
    }

    @Override
    public final synchronized void finishReceived() {
        if (status != null && status.equals(HarvestStatus.FINISHED)) {
            status = HarvestStatus.WAITING;
        }
    }

    @Override
    public abstract String getMessage();
    
    @Override
    public final boolean isUpdated() {
        return updated;
    }

    @Override
    public final void clearUpdated() {
        updated = false;
    }

    @Override
    public abstract void run();

}
