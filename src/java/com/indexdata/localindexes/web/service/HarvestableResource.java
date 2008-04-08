/*
 *  HarvestableResource
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.service;

import javax.persistence.EntityTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import com.indexdata.localindexes.web.converter.HarvestableConverter;
import javax.persistence.PersistenceContext;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 *
 * @author jakub
 */
@PersistenceContext(name = "persistence/localindexes", unitName = "localindexes")
public class HarvestableResource {

    /* *Persistence stuff*
     * There are two ways of doing persistence:
     * - container-managed transactions
     * - application-managed transactions
     * 
     * CMT:
     * requires JTA transaction type (persistrence.xml)
     * uses injected/looked-up EntityManager/EntityManagerFactory
     * uses (injected) UserTransaction API
     * 
     * AMT:
     * one has to handle EntityManagerFactory (expensive) 
     * and EntityManager (cheap) creation
     * (through some thread-safe patterns like thread-local)
     * uses EntityTransaction API retrieved from EM
     * uses RESOURCE_LOCAL transaction type
     */

    // container-managed transactions (em injected)
    @PersistenceUnit(unitName = "localindexes")
    private EntityManagerFactory emf;

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    @Resource
    private UserTransaction utx;

    // container-managed transactions (em looked-up)
//    private EntityManager getEntityManager() {
//        EntityManager em = null;
//        try {
//            em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/localindexes");
//        } catch (NamingException e) {
//            Logger.getLogger(HarvestablesResource.class.getName()).log(Level.SEVERE, null, e);
//        }
//        return em;
//    }
    
    // application-managed transaction
    // private EntityManager getEntityManager() {
    //    return EntityManagerRetriever.getEntityManager();
    // }

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
        /*PersistenceService service = PersistenceService.getInstance();
        try {
        service.beginTx();
        Harvestable entity = getEntity();
        service.removeEntity(entity);
        service.commitTx();
        } finally {
        service.close();
        }*/
        //deleteEntity(retrieveEntity());
        nonJtaDeleteEntity(retrieveEntity());
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
            /*return (Harvestable) PersistenceService.getInstance().createQuery("SELECT e FROM Harvestable e where e.id = :id").setParameter("id", id).getSingleResult();*/
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
        /*newEntity.setId(entity.getId());
        entity = PersistenceService.getInstance().mergeEntity(newEntity);*/
        EntityManager em = getEntityManager();
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
            em.close();
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
        try {
            utx.begin();
            em.joinTransaction();
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
            em.close();
        }
    }

    protected void nonJtaDeleteEntity(Harvestable entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        if (!tx.isActive()) {
            tx.begin();
        }
        em.remove(entity);
        tx.commit();
    }
}
