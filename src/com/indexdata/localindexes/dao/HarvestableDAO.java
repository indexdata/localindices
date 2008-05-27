/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.localindexes.dao;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
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
     * Retrieve list of all harvestables from the Web Service
     * @return
     */
    Collection<HarvestableRefConverter> pollHarvestableRefList();
    /**
     * Retrieve harvestable from the Web Service using it's reference (URL)
     * @param href harvestableRef entity
     * @return harvesatble entity
     */
    Harvestable retrieveFromRef(HarvestableRefConverter href);
}
