package com.indexdata.localindexes.dao;

import com.indexdata.localindexes.web.entity.Harvestable;
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
}
