/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;

/**
 * REST Web service (reource) that maps to a Harvestable entity.
 * @author jakub
 */
public class HarvestableResource {
    private HarvestableDAO dao = new HarvestablesDAOJPA();
    private Long id;
    private UriInfo context;

    /** Creates a new instance of HarvestableResource */
    public HarvestableResource() {
    }

    /**
     * Constructor used for instantiating an instance of the entity refernced by id.
     *
     * @param id identifier for refernced the entity
     * @param context HttpContext inherited from the parent resource
     */
    public HarvestableResource(Long id, UriInfo context) {
        this.id = id;
        this.context = context;
    }

    /**
     * Get method for retrieving an instance of refernced Harvestable in XML format.
     *
     * @return an instance of HarvestableConverter
     */
    @GET
    @ProduceMime({"application/xml", "application/json"})
    public HarvestableConverter get() {
        return new HarvestableConverter(dao.retrieveHarvestableById(id), context.getAbsolutePath());
    }

    /**
     * Put method for updating an instance of refernced Harvestable, using XML as the input format.
     *
     * @param data an HarvestableConverter entity that is deserialized from an XML stream
     */
    @PUT
    @ConsumeMime({"application/xml", "application/json"})
    public void put(HarvestableConverter data) {
        dao.updateHarvestable(dao.retrieveHarvestableById(id), data.getEntity());
    }

    /**
     * Delete method for deleting an instance of referenced Harvestable.
     *
     */
    @DELETE
    public void delete() {
        dao.deleteHarvestable(dao.retrieveHarvestableById(id));
    }
}
