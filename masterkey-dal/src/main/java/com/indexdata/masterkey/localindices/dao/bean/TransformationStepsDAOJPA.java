/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;
import com.indexdata.utils.persistence.EntityUtil;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class TransformationStepsDAOJPA implements TransformationStepDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }
    
    @Override
    public void create(TransformationStep transformation) {
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
    public TransformationStep retrieveById(Long id) {
        EntityManager em = getEntityManager();
        TransformationStep hable = em.find(TransformationStep.class, id);
        em.close();
        return hable;
    }

    @Override
    public TransformationStep update(TransformationStep updTransformation) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        TransformationStep transformation = null;
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
    public void delete(TransformationStep transformation) {
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
    public List<TransformationStep> retrieve(int start, int max) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        // HACK: Hides database errors but does not crash the Scheduler
        List<TransformationStep> hables = new LinkedList<TransformationStep>();
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from TransformationStep as o order by o.name");
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
    public List<TransformationStepBrief> retrieveBriefs(int start, int max) {
        List<TransformationStepBrief> hrefs = new ArrayList<TransformationStepBrief>();
        for (TransformationStep hable : retrieve(start, max)) {
            TransformationStepBrief href = new TransformationStepBrief(hable, null, false);
            hrefs.add(href);
        }
        return hrefs;
    }

    @Override
    public TransformationStep retrieveFromBrief(TransformationStepBrief href) {
        return retrieveById(href.getId());
    }

    public int getCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from TransformationStep as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }    
    }

    @Override
    public List<Transformation> getTransformations(TransformationStep step) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public List<TransformationStep> getEnabledSteps() {
      // TODO Auto-generated method stub
      return null;
    }

  @Override
  public List<TransformationStep> retrieve(int start, int max, String sortKey,
    boolean asc) {
    return retrieve(start, max);
  }

  @Override
  public List<TransformationStepBrief> retrieveBriefs(int start, int max,
    String sortKey, boolean asc) {
    return retrieveBriefs(start, max);
  }
}
