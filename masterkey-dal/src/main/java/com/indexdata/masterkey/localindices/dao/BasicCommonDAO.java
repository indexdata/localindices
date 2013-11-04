/*
 * Copyright (c) 1995-2013, Index Datassss
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

  List<Entity> retrieve(int start, int max);

  List<Entity> retrieve(int start, int max, String sortKey, boolean asc);

  int getCount();
}
