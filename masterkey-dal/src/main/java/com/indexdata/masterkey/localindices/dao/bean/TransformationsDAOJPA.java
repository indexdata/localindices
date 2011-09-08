/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;
import com.indexdata.utils.persistence.EntityUtil;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class TransformationsDAOJPA implements TransformationDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }
    
    @Override
    public void create(Transformation transformation) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(transformation);
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
    public Transformation retrieveById(Long id) {
        EntityManager em = getEntityManager();
        Transformation hable = em.find(Transformation.class, id);
        em.close();
        return hable;
    }

    @Override
    public Transformation update(Transformation updTransformation) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        Transformation transformation = null;
        try {
            tx.begin();
            transformation = em.merge(updTransformation);
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
        return transformation;    
    }    

    @Override
    public void delete(Transformation transformation) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            transformation = em.merge(transformation);
            em.remove(transformation);
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
    public List<Transformation> retrieve(int start, int max) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<Transformation> hables = null;
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from Transformation as o");
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
    public List<TransformationBrief> retrieveBriefs(int start, int max) {
        List<TransformationBrief> hrefs = new ArrayList<TransformationBrief>();
        for (Transformation hable : retrieve(start, max)) {
            TransformationBrief href = new TransformationBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    @Override
    public Transformation retrieveFromBrief(TransformationBrief href) {
        return retrieveById(href.getId());
    }

    @Override
    public int getCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Transformation as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }    
    }
}
