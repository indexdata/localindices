/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
