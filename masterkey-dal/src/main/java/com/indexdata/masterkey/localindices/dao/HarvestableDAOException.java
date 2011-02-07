/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.dao;

/**
 * Non-specific DAO exception.
 * @author jakub
 */
public class HarvestableDAOException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8190922003489241944L;

	public HarvestableDAOException(String msg) {
        super(msg);
    }

}
