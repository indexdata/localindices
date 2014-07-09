/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import java.io.InputStream;
import java.util.Date;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO extends CommonDAO<Harvestable, HarvestableBrief> {
	public InputStream getLog(long id, Date from) throws DAOException;
	public InputStream reset(long id);
        public void resetCache(long id) throws DAOException;
}
