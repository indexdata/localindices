/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.util.HarvestableLog;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;

/**
 * REST Web service (resource) that maps to a Harvestable entity.
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
     * Constructor used for instantiating an instance of the entity referenced by id.
     *
     * @param id identifier for referenced the entity
     * @param context HttpContext inherited from the parent resource
     */
    public HarvestableResource(Long id, UriInfo context) {
        this.id = id;
        this.context = context;
    }

    /**
     * Get method for retrieving an instance of referenced Harvestable in XML format.
     *
     * @return an instance of HarvestableConverter
     */
    @GET
    @Produces("application/xml")
    public HarvestableConverter get() {
        return new HarvestableConverter(dao.retrieveHarvestableById(id), context.getAbsolutePath());
    }

    /**
     * Put method for updating an instance of referenced Harvestable, using XML as the input format.
     *
     * @param data an HarvestableConverter entity that is deserialized from an XML stream
     */
    @PUT
    @Consumes("application/xml")
    public void put(HarvestableConverter data) {
        Harvestable entity = data.getEntity();
        entity.setCurrentStatus("NEW");
        entity.setMessage(null);
        dao.updateHarvestable(entity);
    }

    /**
     * Delete method for deleting an instance of referenced Harvestable.
     *
     */
    @DELETE
    public void delete() {
        dao.deleteHarvestable(dao.retrieveHarvestableById(id));
    }

    /**
     * Entry point to the Harvestable log file.
     *
     */
    @Path("log/")
    @GET
    @Produces("text/plain")
    public String getHarvestableLog() {
        try {
            return HarvestableLog.getHarvestableLog(id);
        } catch (FileNotFoundException fnf) {
            throw new WebApplicationException(fnf);
        } catch (IOException io) {
            throw new WebApplicationException(io);
        }
    }
}
