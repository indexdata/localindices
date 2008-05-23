/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.masterkey.harvest.oai.HarvestStatus;

/**
 *
 * @author jakub
 */
public class JobInfo {
    private Harvestable harvestable;
    private HarvestStatus status;
    private String error;
    private String harvestPeriod;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getHarvestPeriod() {
        return harvestPeriod;
    }

    public void setHarvestPeriod(String harvestPeriod) {
        this.harvestPeriod = harvestPeriod;
    }
    
    public Harvestable getHarvestable() {
        return harvestable;
    }

    public void setHarvestable(Harvestable harvestable) {
        this.harvestable = harvestable;
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStatus(HarvestStatus status) {
        this.status = status;
    }
}
