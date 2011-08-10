/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO {
    public void createHarvestable(Harvestable entity);
    public Harvestable retrieveHarvestableById(Long id);
    public Harvestable updateHarvestable(Harvestable entity);
    public void deleteHarvestable(Harvestable entity);
    public List<Harvestable> retrieveHarvestables(int start, int max);
    public int getHarvestableCount();
    public InputStream getHarvestableLog(long id);
    /**
     * Retrieve a list of brief (listing) harvestables.
     * @return
     */
    List<HarvestableBrief> retrieveHarvestableBriefs(int start, int max);
    /**
     * Retrieves a harvestable using it's listing reference (brief)
     * @param hbrief brief (listing) harvestable
     * @return harvestable detailed harvestable
     */
    Harvestable retrieveFromBrief(HarvestableBrief hbrief);
}
