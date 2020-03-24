package com.indexdata.masterkey.localindices.dao;

import java.util.List;

public interface CommonDAO<Entity, EntityBrief> extends BasicCommonDAO<Entity> {
  /**
   * Retrieve a list of brief (listing) harvestables.
   *
   * @return
   */
  List<EntityBrief> retrieveBriefs(int start, int max, EntityQuery query);

  List<EntityBrief> retrieveBriefs(int start, int max, String sortKey,
    boolean asc, EntityQuery query);

  /**
   * Convert from EntityBrief to Entity
   *
   * @param brief
   * @return
   */
  Entity retrieveFromBrief(EntityBrief brief);
}
