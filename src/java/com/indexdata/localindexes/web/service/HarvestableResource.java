/*
 *  HarvestableResource
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.service;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.WebApplicationException;
import javax.persistence.NoResultException;
import com.indexdata.localindexes.web.converter.HarvestableConverter;
import javax.ws.rs.core.UriInfo;


/**
 *
 * @author jakub
 */

public class HarvestableResource {
    private Long id;
    private UriInfo context;
    
    /** Creates a new instance of HarvestableResource */
    public HarvestableResource() {
    }

    /**
     * Constructor used for instantiating an instance of dynamic resource.
     *
     * @param context HttpContext inherited from the parent resource
     */
    public HarvestableResource(Long id, UriInfo context) {
        this.id = id;
        this.context = context;
    }

    /**
     * Get method for retrieving an instance of Harvestable identified by id in XML format.
     *
     * @param id identifier for the entity
     * @return an instance of HarvestableConverter
     */
    @GET
    @ProduceMime({"application/xml", "application/json"})
    public HarvestableConverter get() {
        try {
            return new HarvestableConverter(getEntity(), context.getAbsolutePath());
        } finally {
            PersistenceService.getInstance().close();
        }
    }

    /**
     * Put method for updating an instance of Harvestable identified by id using XML as the input format.
     *
     * @param id identifier for the entity
     * @param data an HarvestableConverter entity that is deserialized from a XML stream
     */
    @PUT
    @ConsumeMime({"application/xml", "application/json"})
    public void put(HarvestableConverter data) {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            updateEntity(getEntity(), data.getEntity());
            service.commitTx();
        } finally {
            service.close();
        }
    }

    /**
     * Delete method for deleting an instance of Harvestable identified by id.
     *
     * @param id identifier for the entity
     */
    @DELETE
    public void delete() {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            Harvestable entity = getEntity();
            service.removeEntity(entity);
            service.commitTx();
        } finally {
            service.close();
        }
    }

    /**
     * Returns an instance of Harvestable identified by id.
     *
     * @param id identifier for the entity
     * @return an instance of Harvestable
     */
    protected Harvestable getEntity() {
        try {
            return (Harvestable) PersistenceService.getInstance().createQuery("SELECT e FROM Harvestable e where e.id = :id").setParameter("id", id).getSingleResult();
        } catch (NoResultException ex) {
            throw new WebApplicationException(new Throwable("Resource for " + context.getAbsolutePath() + " does not exist."), 404);
        }
    }

    /**
     * Updates entity using data from newEntity.
     *
     * @param entity the entity to update
     * @param newEntity the entity containing the new data
     * @return the updated entity
     */
    protected Harvestable updateEntity(Harvestable entity, Harvestable newEntity) {
        newEntity.setId(entity.getId());
        entity = PersistenceService.getInstance().mergeEntity(newEntity);
        return entity;
    }
}
