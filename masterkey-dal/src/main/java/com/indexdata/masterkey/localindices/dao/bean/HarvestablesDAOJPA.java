/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.utils.persistence.EntityUtil;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class HarvestablesDAOJPA implements HarvestableDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

  private final static List<String> filterByColumns = Arrays.asList(
              "name",
              "description",
              "technicalNotes",
              "contactNotes",
              "serviceProvider",
              "usedBy",
              "managedBy",
              "currentStatus");

  @Override
  public List<Harvestable> retrieve(int start, int max, EntityQuery query) {
    return retrieve(start, max, null, true, query);
  }

  @Override
  public List<HarvestableBrief> retrieveBriefs(int start, int max, EntityQuery query) {
    return retrieveBriefs(start, max, null, true, query);
  }
    public enum AllowedSortKey {
      NAME("name", "o.name"),
      STATUS("currentStatus", "o.currentStatus"),
      DATE_STARTED("lastHarvestStarted", "o.lastHarvestStarted"),
      DATE_FINISHED("lastHarvestFinished", "o.lastHarvestFinished"),
      DATE_STARTED_OR_FINISHED("lastHarvestStartedOrFinished", "coalesce(o.lastHarvestFinished, o.lastHarvestStarted)");
      private String sortKey;
      private String orderExpression;
      AllowedSortKey(String sortKey, String orderExpression) {
        this.sortKey = sortKey;
        this.orderExpression = orderExpression;
      }
      public String getOrderExpression() {
        return orderExpression;
      }
      public String getSortKey() {
        return sortKey;
      }
      public static List<AllowedSortKey> fromString(String spec) {
        if (spec != null) {
          String[] fieldNames = spec.split(",");
          List<AllowedSortKey> allowed = new ArrayList<AllowedSortKey>(fieldNames.length);
          for (String fieldName : fieldNames) {
            for (AllowedSortKey em : values()) {
              if (fieldName.equalsIgnoreCase(em.sortKey))
                allowed.add(em);
            }
          }
          return allowed;
        }
        return null;
      }
    }
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }

    @Override
    public void create(Harvestable harvestable) {
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
    public Harvestable retrieveById(Long id) {
        EntityManager em = getEntityManager();
        Harvestable hable = em.find(Harvestable.class, id);
        em.close();
        return hable;
    }

    @Override
    public Harvestable update(Harvestable updHarvestable) {
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
    public void delete(Harvestable harvestable) {
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

    @Override
    public List<Harvestable> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
      EntityManager em = getEntityManager();
      EntityTransaction tx = em.getTransaction();
      // Communication errors with the persistence Layer will now just look like 0 records exists.
      // But at least our Scheduler wont stop running.
      List<Harvestable> hables = null;
      try {
          tx.begin();
          String orderBy = "o.name";
          List<AllowedSortKey> sks =  AllowedSortKey.fromString(sortKey);
          if (sks != null && !sks.isEmpty()) {
            orderBy = "";
            String sep = "";
            for (AllowedSortKey sk : sks) {
              orderBy += sep + sk.getOrderExpression();
              sep = ", ";
            }
          }
          orderBy += (asc ? " ASC" : " DESC");
          String whereClause = query.asWhereClause(filterByColumns,"o",true);
          String qs = "select object(o) from Harvestable as o " + whereClause + " order by "
            + orderBy;
          Query q = em.createQuery(qs);
          q.setMaxResults(max);
          q.setFirstResult(start);
          hables = q.getResultList();
          tx.commit();
      } catch (Exception ex) {
          logger.log(Level.ERROR, "Failed to select Harvestable(s)", ex);
          try {
              tx.rollback();
          } catch (Exception e) {
              logger.log(Level.DEBUG, e);
          }
          // Some sort of analysis on the exception is required.
          // Temporary should either ignored or throw as Interrupted
          // Fatals should be re-thrown.
          // For now every one is logged but otherwise ignored.
      } finally {
          em.close();
      }
      return hables;
    }

    @Override
    public int getCount(EntityQuery query) {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Harvestable as o " + query.asWhereClause(filterByColumns, "o", true)).getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }
    }


    @Override
    public List<HarvestableBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc, EntityQuery query) {
        List<HarvestableBrief> hrefs = new ArrayList<HarvestableBrief>();
        List<Harvestable> list = retrieve(start, max, sortKey, asc, query);
        if (list == null)
          return null;
        for (Harvestable hable : list) {
            HarvestableBrief href = new HarvestableBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    @Override
    public Harvestable retrieveFromBrief(HarvestableBrief href) {
        return retrieveById(href.getId());
    }

    @Override
    public InputStream getLog(long id, Date from) throws DAOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream reset(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

  @Override
  public void resetCache(long id) throws DAOException {
    throw new UnsupportedOperationException("DiskCache removal must be performed through the web service.");
  }
}
