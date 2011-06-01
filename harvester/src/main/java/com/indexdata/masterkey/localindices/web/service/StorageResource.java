/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.bean.StoragesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;

/**
 * REST Web service (resource) that maps to a Storage entity.
 * @author Dennis
 */
public class StorageResource {
    private StorageDAO dao = new StoragesDAOJPA();
    private Long id;
    private UriInfo context;

    /** Creates a new instance of HarvestableResource */
    public StorageResource() {
    }

    /**
     * Constructor used for instantiating an instance of the entity referenced by id.
     *
     * @param id identifier for referenced the entity
     * @param context HttpContext inherited from the parent resource
     */
    public StorageResource(Long id, UriInfo context) {
        this.id = id;
        this.context = context;
    }

    /**
     * Get method for retrieving an instance of referenced Storage in XML format.
     *
     * @return an instance of StorageConverter
     */
    @GET
    @Produces("application/xml")
    public StorageConverter get() {
        return new StorageConverter(dao.retrieveStorageById(id), context.getAbsolutePath());
    }

    /**
     * Put method for updating an instance of refernced Harvestable, using XML as the input format.
     *
     * @param data an HarvestableConverter entity that is deserialized from an XML stream
     */
    @PUT
    @Consumes("application/xml")
    public void put(StorageConverter data) {
        Storage entity = data.getEntity();
        // TODO Fix
        entity.setCurrentStatus("NEW");
        entity.setMessage(null);
        dao.updateStorage(entity);
    }

    /**
     * Delete method for deleting an instance of referenced Harvestable.
     *
     */
    @DELETE
    public void delete() {
        dao.deleteStorage(dao.retrieveStorageById(id));
    }

}
