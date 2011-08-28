/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import javax.servlet.ServletContext;

import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationsDAOJPA;

/**
 * Creates the the StorageDAO object based on the current context.
 * @author jakub
 */
public class TransformationStepAssociationDAOFactory {
    /**
     * Create an instance of the DAO<T> given current context.
     * @param ctx current context
     * @return and instance of the DAO
     * @throws com.indexdata.masterkey.localindices.dao.StorageDAOException
     */
    public  static TransformationStepAssociationDAO getDAO(ServletContext ctx) throws DAOException {
        //class identifier, here simply a package name
        String daoParamValue = lookupContext(ctx, "com.indexdata.masterkey.localindices.TransformationStepAssociationDAO");
        if ("TransformationStepAssociationDAOJPA".equals(daoParamValue)) {
            return new TransformationStepAssociationsDAOJPA();
        } else if ("TransformationStepAssociationDAOWS".equals(daoParamValue)) {
            String baseUrl = lookupContext(ctx, "com.indexdata.masterkey.localindices.TransformationStepAssociationDAO.WS_BASE_URL");
            return new TransformationStepAssociationDAOWS(baseUrl);
        } else if ("TransformationStepAssociationDAOFake".equals(daoParamValue)) {
            return new TransformationStepAssociationDAOFake();
        }
        throw new DAOException("Cannot create TransformationStepAssociationDAO for corresponding parameter value " + daoParamValue);
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
