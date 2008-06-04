/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;

/**
 * Returns an instance of a HarvestStorage object.
 * @author jakub
 */
public class HarvestStorageFactory {
    public HarvestStorageFactory() {
        
    }
    public static HarvestStorage getSotrage(Harvestable harvestable) {
        return new ZebraFileStorage("/tmp/harvested", harvestable);
    }
}
