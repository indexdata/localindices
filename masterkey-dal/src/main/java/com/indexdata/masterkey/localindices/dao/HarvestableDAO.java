/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO extends CommonDAO<Harvestable, HarvestableBrief> {
	public InputStream getLog(long id, Date from) throws DAOException;
	public InputStream reset(long id);
        public void resetCache(long id) throws DAOException;
  List<Harvestable> retrieve(int start, int max, String sortKey, boolean asc, String filterString);
  public List<HarvestableBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc, String filterString);
  public int getCount(String filterString);
}
