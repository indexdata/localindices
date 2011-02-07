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
import java.io.InputStream;
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
    
    @Override
    public void createHarvestable(Harvestable harvestable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            harvestable.setCurrentStatus("NEW");
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

    @Override
    public Harvestable retrieveHarvestableById(Long id) {
        EntityManager em = getEntityManager();
        Harvestable hable = em.find(Harvestable.class, id);
        em.close();
        return hable;
    }

    @Override
    public Harvestable updateHarvestable(Harvestable updHarvestable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        Harvestable harvestable = null;
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

    @Override
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

    @SuppressWarnings("unchecked")
	@Override
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

    @Override
    public int getHarvestableCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Harvestable as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }    
    }

    @Override
    public List<HarvestableBrief> retrieveHarvestableBriefs(int start, int max) {
        List<HarvestableBrief> hrefs = new ArrayList<HarvestableBrief>();
        for (Harvestable hable : retrieveHarvestables(start, max)) {
            HarvestableBrief href = new HarvestableBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    @Override
    public Harvestable retrieveFromBrief(HarvestableBrief href) {
        return retrieveHarvestableById(href.getId());
    }

    @Override
    public InputStream getHarvestableLog(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
