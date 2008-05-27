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

/**
 *
 * @author jakub
 */
@Path("/harvestables/")
public class HarvestablesResource {
    private HarvestableDAO dao = new HarvestablesDAOJPA();
    @Context
    private UriInfo context;

    /** Creates a new instance of HarvestablesResource */
    public HarvestablesResource() {
    }

    /**
     * Constructor used for instantiating an instance of dynamic resource.
     *
     * @param context HttpContext inherited from the parent resource
     */
    public HarvestablesResource(UriInfo context) {
        this.context = context;
    }

    /**
     * Get method for retrieving a collection of Harvestable instance in XML format.
     *
     * @return an instance of HarvestablesConverter
     */
    @GET
    @ProduceMime({"application/xml", "application/json"})
    public HarvestablesConverter get(
            
            @QueryParam("start")
            @DefaultValue("0") int start,
            
            @QueryParam("max")
            @DefaultValue("10") int max) {
        return new HarvestablesConverter(dao.retrieveHarvestables(start, max), context.getAbsolutePath());
    }

    /**
     * Post method for creating an instance of Harvestable using XML as the input format.
     *
     * @param data an HarvestableConverter entity that is deserialized from an XML stream
     * @return an instance of HarvestableConverter
     */
    @POST
    @ConsumeMime({"application/xml", "application/json"})
    public Response post(HarvestableConverter data) {
        Harvestable entity = data.getEntity();
        dao.createHarvestable(entity);
        return Response.created(context.getAbsolutePath().resolve(entity.getId() + "/")).build();
    }

    /**
     * Returns a dynamic instance of HarvestableResource used for entity navigation.
     *
     * @param id resource id
     * @return an instance of HarvestableResource
     */
    @Path("{id}/")
    public HarvestableResource getHarvestableResource(            
    @PathParam("id") Long id) {
        return new HarvestableResource(id, context);
    }
}
