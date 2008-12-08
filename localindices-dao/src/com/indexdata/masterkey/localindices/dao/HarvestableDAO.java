/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import java.util.Collection;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO {
    public void createHarvestable(Harvestable harvestable);
    public Harvestable retrieveHarvestableById(Long id);
    public Harvestable updateHarvestable(Harvestable harvestable, Harvestable updHarvestable);
    public Harvestable updateHarvestable(Harvestable hable);
    public void deleteHarvestable(Harvestable harvestable);
    public Collection<Harvestable> retrieveHarvestables(int start, int max);
    public int getHarvestableCount();    
    /**
     * Retrieve a list of brief (listing) harvestables.
     * @return
     */
    Collection<HarvestableBrief> retrieveHarvestableBriefs(int start, int max);
    /**
     * Retrieves a harvestable using it's listing reference (brief)
     * @param hbrief brief (listing) harvestable
     * @return harvesatble detailed harvestable
     */
    Harvestable retrieveFromBrief(HarvestableBrief hbrief);
}
