/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.localindexes.scheduler.dao;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import java.util.Collection;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO {

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

    /**
     * PUT harvestable to the Web Service
     * @param harvestable entity to be put
     */
    void updateHarvestable(Harvestable harvestable);

}
