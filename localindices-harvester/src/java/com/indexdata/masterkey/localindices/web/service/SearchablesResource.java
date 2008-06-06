/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import com.indexdata.masterkey.localindices.web.service.converter.SearchablesConverter;

/**
 * RESTful WS (resource) that maps to the Searchables collection.
 * @author jakub
 */
@Path("/searchables/")
public class SearchablesResource {
    private HarvestableDAO dao = new HarvestablesDAOJPA();
    @Context
    private UriInfo context;

    /** Creates a new instance of HarvestablesResource */
    public SearchablesResource() {
    }

    /**
     * Constructor used for instantiating an instance of the Searchable resource.
     *
     * @param context HttpContext inherited from the parent resource
     */
    public SearchablesResource(UriInfo context) {
        this.context = context;
    }

    /**
     * Get method for retrieving a collection of Searchables instance in XML format.
     *
     * @param start optional start item argument
     * @param max optional max results argument
     * @return an instance of Searchables
     */
    @GET
    @ProduceMime({"application/xml", "application/json"})
    public SearchablesConverter get(
            
            @QueryParam("start")
            @DefaultValue("0") int start,
            
            @QueryParam("max")
            @DefaultValue("10") int max) {
        return new SearchablesConverter(dao.retrieveHarvestables(start, max), context.getAbsolutePath());
    }
}
