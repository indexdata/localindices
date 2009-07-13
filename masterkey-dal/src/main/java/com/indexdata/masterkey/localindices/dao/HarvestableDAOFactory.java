/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.dao;

import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import javax.servlet.ServletContext;

/**
 * Creates the the HarvestableDAO object based on the current context.
 * @author jakub
 */
public class HarvestableDAOFactory {
    /**
     * Create an instance of the HarvestableDAO given current context.
     * @param ctx current context
     * @return and instance of the HarvestableDAO
     * @throws com.indexdata.masterkey.localindices.dao.HarvestableDAOException
     */
    public static HarvestableDAO getHarvestableDAO(ServletContext ctx) throws HarvestableDAOException {
        //class identifier, here simply a package name
        String daoParamValue = lookupContext(ctx, "com.indexdata.masterkey.localindices.HarvestableDAO");
        if (daoParamValue.equals("HarvestableDAOJPA")) {
            return new HarvestablesDAOJPA();
        } else if (daoParamValue.equals("HarvestableDAOWS")) {
            String baseUrl = lookupContext(ctx, "com.indexdata.masterkey.localindices.HarvestableDAO.WS_BASE_URL");
            return new HarvestableDAOWS(baseUrl);
        } else if (daoParamValue.equals("HarvestableDAOFake")) {
            return new HarvestableDAOFake();
        }
        throw new HarvestableDAOException("Cannot create HarvestableDAO for corresponding parameter " + daoParamValue);
    }
    
    /**
     * Lookup the context for string parameters. Try the attribute first, 
     * then fallback to the init param.
     * @param ctx current context
     * @param paramName parameter name
     * @return parameter value
     * @throws com.indexdata.masterkey.localindices.dao.HarvestableDAOException
     */
    private static String lookupContext(ServletContext ctx, String paramName) throws HarvestableDAOException {
        String daoParamValue = (String) ctx.getAttribute(paramName);
        if (daoParamValue == null) 
                daoParamValue = ctx.getInitParameter(paramName);
        else 
            throw new HarvestableDAOException("Parameter " + paramName + " is not specified in the context.");
        return daoParamValue;
    }
}
