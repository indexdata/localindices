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

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationBrief;
import com.indexdata.utils.persistence.EntityUtil;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class TransformationStepAssociationsDAOJPA implements TransformationStepAssociationDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }
    
    @Override
    public void create(TransformationStepAssociation entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
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
    public TransformationStepAssociation retrieveById(Long id) {
        EntityManager em = getEntityManager();
        TransformationStepAssociation hable = em.find(TransformationStepAssociation.class, id);
        em.close();
        return hable;
    }

    @Override
    public TransformationStepAssociation update(TransformationStepAssociation entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        TransformationStepAssociation transformation = null;
        try {
            tx.begin();
            transformation = em.merge(entity);
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
    public void delete(TransformationStepAssociation entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            entity = em.merge(entity);
            em.remove(entity);
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
    public List<TransformationStepAssociation> retrieve(int start, int max, EntityQuery query) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        // HACK: Hides database errors but does not crash the Scheduler
        List<TransformationStepAssociation> hables = new LinkedList<>();
        try {
            tx.begin();
            String queryString = "select object(o) from TransformationStepAssociation as o" + query.asWhereClause("o") + " order by o.transformation.id, o.position";
            logger.info("Query is: " + queryString);
            Query q = em.createQuery(queryString);
            q.setMaxResults(max);
            q.setFirstResult(start);
            hables = q.getResultList();
            tx.commit();
        } catch (Exception ex) {
            logger.log(Level.INFO, ex);
            try {
                tx.rollback();
            } catch (Exception e) {
                logger.log(Level.INFO, e);
            }
        } finally {
            em.close();
        }
        return hables;
    }

	@SuppressWarnings("unchecked")
	@Override
	public List<TransformationStepAssociation> retrieveByTransformationId(Long transformationId) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<TransformationStepAssociation> hables = null;
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from TransformationStepAssociation o where o.transformation.id = :id order by o.position");
            q.setParameter("id", transformationId);
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

	@SuppressWarnings("unchecked")
	@Override
	public List<TransformationStepAssociation> retrieveByStepId(Long stepId) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<TransformationStepAssociation> hables = null;
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from TransformationStepAssociation o where o.step.id = :id");
            q.setParameter("id", stepId);
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
	public int getStepCountByTransformationId(Long id) {
        EntityManager em = getEntityManager();
        try {
        	Query query = em.createQuery("select count(o) from TransformationStepAssociation o where o.transformation.id = :id");
        	query.setParameter("id", id);
        	return ((Long) query.getSingleResult()).intValue();
        } finally {
            em.close();
        }    
	}

	@Override
	public int getTransformationCountByStepId(Long id) {
        EntityManager em = getEntityManager();
        try {
        	Query query = em.createQuery("select count(o) from TransformationStepAssociation o where o.step.id = :id");
        	query.setParameter("id", id);
        	return ((Long) query.getSingleResult()).intValue();
        } finally {
            em.close();
        }    
	}

	@Override
    public List<TransformationStepAssociationBrief> retrieveBriefs(int start, int max, EntityQuery query) {
        List<TransformationStepAssociationBrief> hrefs = new ArrayList<>();
        for (TransformationStepAssociation hable : retrieve(start, max, query)) {
        	TransformationStepAssociationBrief href = new TransformationStepAssociationBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

	@Override
	public TransformationStepAssociation retrieveFromBrief(
			TransformationStepAssociationBrief brief) {
        return retrieveById(brief.getId());
	}

	@Override
	public int getCount(EntityQuery entityQuery) {
        EntityManager em = getEntityManager();
        try {
        	Query query = em.createQuery("select count(o) from TransformationStepAssociation o " + entityQuery.asWhereClause("o"));
        	return ((Long) query.getSingleResult()).intValue();
        } finally {
            em.close();
        }    
	}

  @Override
  public List<TransformationStepAssociation> retrieve(int start, int max,
    String sortKey, boolean asc, EntityQuery query) {
    return retrieve(start, max, query);
  }

  @Override
  public List<TransformationStepAssociationBrief> retrieveBriefs(int start,
    int max, String sortKey, boolean asc, EntityQuery query) {
    return retrieveBriefs(start, max, query);
  }

}
