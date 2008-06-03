/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

public interface HarvestJob extends Runnable {
    
    /** Stop the current job
     * Rolls back the current harvest, deleting those files received so far
     * Leaves the older harvests in place
     * 
     */
    public void kill();
        
    public HarvestStatus getStatus();
    
    public void setStorage(HarvestStorage storage);
    public HarvestStorage getStorage();
    
    public void finishReceived();
    public String getError();
    
}
