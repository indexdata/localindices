/*
 *  HarvestablesResource
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.service;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import java.util.Collection;
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
import com.indexdata.localindexes.web.converter.HarvestablesConverter;
import com.indexdata.localindexes.web.converter.HarvestableConverter;


/**
 *
 * @author jakub
 */

@Path("/harvestables/")
public class HarvestablesResource {
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
    public HarvestablesConverter get(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max) {
        try {
            return new HarvestablesConverter(getEntities(start, max), context.getAbsolutePath());
        } finally {
            PersistenceService.getInstance().close();
        }
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
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            Harvestable entity = data.getEntity();
            createEntity(entity);
            service.commitTx();
            return Response.created(context.getAbsolutePath().resolve(entity.getId() + "/")).build();
        } finally {
            service.close();
        }
    }

    /**
     * Returns a dynamic instance of HarvestableResource used for entity navigation.
     *
     * @return an instance of HarvestableResource
     */
    @Path("{id}/")
    public HarvestableResource getHarvestableResource(@PathParam("id")
    Long id) {
        return new HarvestableResource(id, context);
    }

    /**
     * Returns all the entities associated with this resource.
     *
     * @return a collection of Harvestable instances
     */
    protected Collection<Harvestable> getEntities(int start, int max) {
        return PersistenceService.getInstance().createQuery("SELECT e FROM Harvestable e").setFirstResult(start).setMaxResults(max).getResultList();
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Harvestable entity) {
        PersistenceService.getInstance().persistEntity(entity);
    }
}
