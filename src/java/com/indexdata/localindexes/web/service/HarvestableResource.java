/*
 *  HarvestableResource
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.service;

import javax.naming.InitialContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import javax.annotation.Resource;
import javax.transaction.UserTransaction;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceContext;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.service.converter.HarvestableConverter;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;

/**
 *
 * @author jakub
 */
//@PersistenceContext(name = "persistence/localindexes", unitName = "localindexes")
public class HarvestableResource {

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

    // JTA bean-managed transactions (emf/utx injected)
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
     * Get method for retrieving an instance of Harvestable in XML format, identified by id.
     *
     * @param id identifier for the entity
     * @return an instance of HarvestableConverter
     */
    @GET
    @ProduceMime({"application/xml", "application/json"})
    public HarvestableConverter get() {
        return new HarvestableConverter(retrieveEntity(), context.getAbsolutePath());
    }

    /**
     * Put method for updating an instance of Harvestable identified by id, using XML as the input format.
     *
     * @param id identifier for the entity
     * @param data an HarvestableConverter entity that is deserialized from a XML stream
     */
    @PUT
    @ConsumeMime({"application/xml", "application/json"})
    public void put(HarvestableConverter data) {
        updateEntity(retrieveEntity(), data.getEntity());
    }

    /**
     * Delete method for deleting an instance of Harvestable identified by id.
     *
     * @param id identifier for the entity
     */
    @DELETE
    public void delete() {
        deleteEntity(retrieveEntity());
    }

    /**
     * Returns an instance of Harvestable identified by id.
     *
     * @param id identifier for the entity
     * @return an instance of Harvestable
     */
    protected Harvestable retrieveEntity() {
        EntityManager em = getEntityManager();
        try {
            return em.find(Harvestable.class, id);
        } catch (Exception ex) {
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
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            entity = em.merge(newEntity);
            utx.commit();
        } catch (Exception ex) {
            Logger.getLogger(HarvestableResource.class.getName()).log(Level.SEVERE, null, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                Logger.getLogger(HarvestableResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
        //em.close();
        }
        return entity;
    }

    /**
     * Deletes entity.
     *
     * @param entity the entity to delete
     */
    protected void deleteEntity(Harvestable entity) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            entity = em.merge(entity);
            em.remove(entity);
            utx.commit();
        } catch (Exception ex) {
            Logger.getLogger(HarvestableResource.class.getName()).log(Level.SEVERE, null, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                Logger.getLogger(HarvestableResource.class.getName()).log(Level.SEVERE, null, e);
            }
        } finally {
        //em.close();
        }
    }
}
