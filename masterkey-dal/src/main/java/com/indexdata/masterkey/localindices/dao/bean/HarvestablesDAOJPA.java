/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.utils.persistence.EntityUtil;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class HarvestablesDAOJPA implements HarvestableDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }
    
    public void createHarvestable(Harvestable harvestable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(harvestable);
            tx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                tx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            em.close();
        }    
    }

    public Harvestable retrieveHarvestableById(Long id) {
        EntityManager em = getEntityManager();
        Harvestable hable = em.find(Harvestable.class, id);
        em.close();
        return hable;
    }

    public Harvestable updateHarvestable(Harvestable harvestable, Harvestable updHarvestable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            harvestable = em.merge(updHarvestable);
            tx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                tx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            em.close();
        }
        return harvestable;    
    }
    
    public Harvestable updateHarvestable(Harvestable harvestable) {
        return updateHarvestable(harvestable, harvestable);
    }

    public void deleteHarvestable(Harvestable harvestable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            harvestable = em.merge(harvestable);
            em.remove(harvestable);
            tx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                tx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            em.close();
        }    
    }

    public List<Harvestable> retrieveHarvestables(int start, int max) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<Harvestable> hables = null;
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from Harvestable as o");
            q.setMaxResults(max);
            q.setFirstResult(start);
            hables = q.getResultList();
            tx.commit();
        } catch (Exception ex) {
            logger.log(Level.DEBUG, ex);
            try {
                tx.rollback();
            } catch (Exception e) {
                logger.log(Level.DEBUG, e);
            }
        } finally {
            em.close();
        }
        return hables;
    }

    public int getHarvestableCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Harvestable as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }    
    }

    public List<HarvestableBrief> retrieveHarvestableBriefs(int start, int max) {
        List<HarvestableBrief> hrefs = new ArrayList<HarvestableBrief>();
        for (Harvestable hable : retrieveHarvestables(start, max)) {
            HarvestableBrief href = new HarvestableBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    public Harvestable retrieveFromBrief(HarvestableBrief href) {
        return retrieveHarvestableById(href.getId());
    }

}
