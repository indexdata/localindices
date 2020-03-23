/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.dao;

import java.util.List;

/**
 *
 * @author jakub
 */
public interface BasicCommonDAO<Entity> {
  
  void create(Entity entity);

  Entity retrieveById(Long id);

  Entity update(Entity entity);

  void delete(Entity entity) throws EntityInUse;

  List<Entity> retrieve(int start, int max, EntityQuery query);

  List<Entity> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query);

  int getCount(EntityQuery query);
}
