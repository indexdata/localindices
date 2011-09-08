/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.dao.bean.TransformationStepDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepsDAOJPA;

import javax.servlet.ServletContext;

/**
 * Creates the the StorageDAO object based on the current context.
 * @author jakub
 */
public class TransformationStepDAOFactory {
    /**
     * Create an instance of the StorageDAO given current context.
     * @param ctx current context
     * @return and instance of the StorageDAO
     * @throws com.indexdata.masterkey.localindices.dao.StorageDAOException
     */
    public static TransformationStepDAO getDAO(ServletContext ctx) throws DAOException {
        //class identifier, here simply a package name
        String daoParamValue = lookupContext(ctx, "com.indexdata.masterkey.localindices.TransformationStepDAO");
        if (daoParamValue.equals("TransformationStepDAOJPA")) {
            return new TransformationStepsDAOJPA();
        } else if (daoParamValue.equals("TransformationStepDAOWS")) {
            String baseUrl = lookupContext(ctx, "com.indexdata.masterkey.localindices.TransformationStepDAO.WS_BASE_URL");
            return new TransformationStepDAOWS(baseUrl);
        } else if (daoParamValue.equals("TransformationStepDAOFake")) {
            return new TransformationStepDAOFake();
        }
        throw new DAOException("Cannot create TransformationDAO for corresponding parameter " + daoParamValue);
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
