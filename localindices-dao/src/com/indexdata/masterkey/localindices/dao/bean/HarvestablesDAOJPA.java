/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableRefConverter;
import java.util.ArrayList;
import java.util.Collection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
//@PersistenceContext(name = "persistence/localindicesPU", unitName = "localindicesPU")
public class HarvestablesDAOJPA implements HarvestableDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
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
//    @PersistenceUnit(unitName = "localindicesPU")
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
            em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/localindicesPU");
        } catch (NamingException e) {
            logger.log(Level.DEBUG, e);
        }
        return em;
    }

    private UserTransaction getUserTransaction() {
        UserTransaction utx = null;
        try {
            utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException e) {
            logger.log(Level.DEBUG, e);
        }
        return utx;
    }
    
    public void createHarvestable(Harvestable harvestable) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            em.persist(harvestable);
            utx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            //em.close();
        }    
    }

    public Harvestable retrieveHarvestableById(Long id) {
        EntityManager em = getEntityManager();
        //try {
            return em.find(Harvestable.class, id);
        //} catch () {
          //  throw new WebApplicationException(new Throwable("Resource for " + context.getAbsolutePath() + " does not exist."), 404);
        //}
    }

    public Harvestable updateHarvestable(Harvestable harvestable, Harvestable updHarvestable) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            harvestable = em.merge(updHarvestable);
            utx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
        //em.close();
        }
        return harvestable;    
    }
    
    public Harvestable updateHarvestable(Harvestable harvestable) {
        return updateHarvestable(harvestable, harvestable);
    }

    public void deleteHarvestable(Harvestable harvestable) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        try {
            utx.begin();
            em.joinTransaction();
            harvestable = em.merge(harvestable);
            em.remove(harvestable);
            utx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
        //em.close();
        }    
    }

    public Collection<Harvestable> retrieveHarvestables(int start, int max) {
        EntityManager em = getEntityManager();
        UserTransaction utx = getUserTransaction();
        Collection<Harvestable> hables = null;
        try {
            utx.begin();
            em.joinTransaction();
            Query q = em.createQuery("select object(o) from Harvestable as o");
            q.setMaxResults(max);
            q.setFirstResult(start);
            hables = q.getResultList();
            utx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                utx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            //em.close();
        }
        return hables;
    }

    public int getHarvestableCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Harvestable as o").getSingleResult()).intValue();
            return count;
        } finally {
            //em.close();
        }    
    }

    public Collection<HarvestableRefConverter> pollHarvestableRefList(int start, int max) {
        Collection<HarvestableRefConverter> hrefs = new ArrayList<HarvestableRefConverter>();
        for (Harvestable hable : retrieveHarvestables(start, max)) {
            HarvestableRefConverter href = new HarvestableRefConverter(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    public Harvestable retrieveFromRef(HarvestableRefConverter href) {
        return retrieveHarvestableById(href.getId());
    }

}
