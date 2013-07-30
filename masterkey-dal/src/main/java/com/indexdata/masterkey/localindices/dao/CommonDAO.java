package com.indexdata.masterkey.localindices.dao;

import java.util.List;

public interface CommonDAO<Entity,EntityBrief> 
{
	void   		 create(Entity entity);
    Entity 		 retrieveById(Long id);
    Entity 		 update(Entity entity);
    void   		 delete(Entity entity);
    /**
     * Retrieve a list of harvestables.
     * @return
     */
    List<Entity> retrieve(int start, int max);
    List<Entity> retrieve(int start, int max, String sortKey, boolean asc);
    /**
     * Retrieve a list of brief (listing) harvestables.
     * @return
     */
    List<EntityBrief> retrieveBriefs(int start, int max);
    List<EntityBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc);
    /**
     * Convert from EntityBrief to Entity
     * @param brief
     * @return
     */
    Entity retrieveFromBrief(EntityBrief brief);
    int getCount();
}
