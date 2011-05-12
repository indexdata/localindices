/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.dao.bean.StorageDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.StoragesDAOJPA;

import javax.servlet.ServletContext;

/**
 * Creates the the StorageDAO object based on the current context.
 * @author jakub
 */
public class StorageDAOFactory {
    /**
     * Create an instance of the StorageDAO given current context.
     * @param ctx current context
     * @return and instance of the StorageDAO
     * @throws com.indexdata.masterkey.localindices.dao.StorageDAOException
     */
    public static StorageDAO getStorageDAO(ServletContext ctx) throws DAOException {
        //class identifier, here simply a package name
        String daoParamValue = lookupContext(ctx, "com.indexdata.masterkey.localindices.StorageDAO");
        if (daoParamValue.equals("StorageDAOJPA")) {
            return new StoragesDAOJPA();
        } else if (daoParamValue.equals("StorageDAOWS")) {
            String baseUrl = lookupContext(ctx, "com.indexdata.masterkey.localindices.StorageDAO.WS_BASE_URL");
            return new StorageDAOWS(baseUrl);
        } else if (daoParamValue.equals("StorageDAOFake")) {
            return new StorageDAOFake();
        }
        throw new DAOException("Cannot create StorageDAO for corresponding parameter " + daoParamValue);
    }
    
    /**
     * Lookup the context for string parameters. Try the attribute first, 
     * then fallback to the init param.
     * @param ctx current context
     * @param paramName parameter name
     * @return parameter value
     * @throws com.indexdata.masterkey.localindices.dao.StorageDAOException
     */
    private static String lookupContext(ServletContext ctx, String paramName) throws DAOException {
        String daoParamValue = (String) ctx.getAttribute(paramName);
        if (daoParamValue == null) 
                daoParamValue = ctx.getInitParameter(paramName);
        else 
            throw new DAOException("Parameter " + paramName + " is not specified in the context.");
        return daoParamValue;
    }
}
