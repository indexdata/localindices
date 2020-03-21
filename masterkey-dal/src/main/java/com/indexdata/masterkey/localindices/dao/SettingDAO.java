/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.entity.Setting;

/**
 *
 * @author jakub
 */
public interface SettingDAO extends BasicCommonDAO<Setting> {
  public int getCount(String prefix, EntityQuery query);
}
