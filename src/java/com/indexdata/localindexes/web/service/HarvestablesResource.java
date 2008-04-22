/*
 * HarvestablesResource
 *
 * Created on April 4, 2008, 12:06 PM
 *
 */
package com.indexdata.localindexes.web.service;

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

import javax.annotation.Resource;
import javax.transaction.UserTransaction;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.persistence.PersistenceContext;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableConverter;
import com.indexdata.localindexes.web.service.converter.HarvestablesConverter;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author jakub
 */
//@PersistenceContext(name = "persistence/localindexes", unitName = "localindexes")
@Path("/harvestables/")
public class HarvestablesResource {

    /* *Persistence stuff*
     * There are two ways of doing persistence:
     * - container-managed transactions
     * - bean-managed transactions (or application-managed tx)
     * 
     * CMT:
     * requires JTA transaction-type (persistrence.xml)
     * uses injected/looked-up EntityManager (through @PersistenceContext)
     * uses injected/looked-up UserTransaction (through @Resource)
     * EntityManger is nerver closed in the finally block
     * 
     * BMT:
     * here we have two more options:
     * -JTA transaction-type (UserTransaction API):
     * -- injected EntityManagerFactory (through @PersistenceUnit)
     * -- injected UserTransaction (throough @Resource)
     * -- EM has to be closes when needed
     * (through )
     * -RESOURCE_LOCAL transaction-type (EntityTransaction API):
     * -- EntityManagerFactory has to be created ( Persistence.createEntityManagerFactory(PU) )
     *    - expensive - and EntityManager has to be managed through some 
     *     thread-safe patterns like thread-local session.
     * -- uses EntityTransaction, retrieved from EM by getTransaction
     * -- EM has to be closed when needed
     */

    // JTA bean-managed transactions (em/utx injected)
    // this how it should work
//    @PersistenceUnit(unitName = "localindexes")
//    private EntityManagerFactory emf;
//
//    private EntityManager getEntityManager() {
//        return emf.createEntityManager();
//    }
//    @Resource
//    private UserTransaction utx;
//
//    private UserTransaction getUserTransaction() {
//        return this.utx;
//    }

    // container-managed transactions (em/utx looked-up)
    // works for now
    private EntityManager getEntityManager() {
        EntityManager em = null;
        try {
            em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/localindexes");
        } catch (NamingException e) {
            Logger.getLogger(HarvestablesResource.class.getName()).log(Level.SEVERE, null, e);
        }
        return em;
    }

    private UserTransaction getUserTransaction() {
        UserTransaction utx = null;
        try {
            utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException e) {
            Logger.getLogger(HarvestablesResource.class.getName()).log(Level.SEVERE, null, e);
        }
        return utx;
    }

    /* persistence stuff ends */
    
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
        return new HarvestablesConverter(retrieveEntities(start, max), context.getAbsolutePath());
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
        createEntity(entity);
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

    /**
     * Returns all the entities associated with this resource.
     *
     * @param start 
     * @param max 
     * @return a collection of Harvestable instances
     */
    protected Collection<Harvestable> retrieveEntities(int start, int max) {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("select object(o) from Harvestable as o");
            q.setMaxResults(max);
            q.setFirstResult(start);
            return q.getResultList();
        } finally {
        //em.close();
        }
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Harvestable entity) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            em.persist(entity);
            utx.commit();
        } catch (Exception ex) {
            Logger.getLogger(HarvestablesResource.class.getName()).log(Level.SEVERE, null, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                Logger.getLogger(HarvestablesResource.class.getName()).log(Level.SEVERE, null, e);
            }
        } finally {
            //em.close();
        }
    }
}
