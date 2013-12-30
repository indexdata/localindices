/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import java.io.InputStream;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;

/**
 *
 * @author jakub
 */
public interface HarvestableDAO extends CommonDAO<Harvestable, HarvestableBrief> {
	public InputStream getLog(long id);
	public InputStream reset(long id);
        public void resetCache(long id) throws DAOException;
        public Harvestable command(Harvestable entity, String cmd);


}
