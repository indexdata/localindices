/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.dao.bean;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.persistence.exceptions.DatabaseException;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.utils.persistence.EntityUtil;

/**
 *
 * @author jakub
 */
/**
 * Java Persistence API implementation of the DAO
 *
 * @author jakub
 */
public class SettingDAOJPA implements SettingDAO {
  private static Logger logger = Logger.getLogger(
    "com.indexdata.masterkey.harvester.dao");

  private EntityManager getEntityManager() {
    return EntityUtil.getManager();
  }

  @Override
  public void create(Setting storage) {
    EntityManager em = getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(storage);
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
  public Setting retrieveById(Long id) {
    EntityManager em = getEntityManager();
    Setting entity = em.find(Setting.class, id);
    em.close();
    return entity;
  }

  @Override
  public Setting update(Setting updEntity) {
    EntityManager em = getEntityManager();
    EntityTransaction tx = em.getTransaction();
    Setting entity = null;
    try {
      tx.begin();
      entity = em.merge(updEntity);
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
    return entity;
  }

  @Override
  public void delete(Setting entity) throws EntityInUse {
    EntityManager em = getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      entity = em.merge(entity);
      em.remove(entity);
      tx.commit();
    } catch (Exception ex) {
      logger.log(Level.DEBUG, ex);
      if (ex.getCause() instanceof DatabaseException) {
        DatabaseException de = (DatabaseException) ex.getCause();
        if ("MySQLIntegrityConstraintViolationException".equals(de.
          getInternalException().getClass().getSimpleName())) {
          throw new EntityInUse("cannot remove storage because it's in use", de.
            getInternalException());
        }
      }
      try {
        tx.rollback();
      } catch (Exception e) {
        logger.log(Level.DEBUG, e);
      }
    } finally {
      em.close();
    }
  }

  /**
   * @param start
   * @param max
   * @param sortKey unused
   * @param asc unused
   * @param query optional criteria, used for SQL WHERE clause if any
   * @return
   */
  @Override
  public List<Setting> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
    EntityManager em = getEntityManager();
    EntityTransaction tx = em.getTransaction();
    List<Setting> entities = null;
    try {
      tx.begin();
      Query q;
      String qry = "select object(o) from Setting as o " + query.asWhereClause("o") + " order by o.name ";
      q = em.createQuery(qry);
      q.setMaxResults(max);
      q.setFirstResult(start);
      entities = q.getResultList();
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
    return entities;
  }

  @Override
  public List<Setting> retrieve(int start, int max, EntityQuery query) {
    return retrieve(start,max,null,false,query);
  }

  @Override
  public int getCount(EntityQuery query) {
    EntityManager em = getEntityManager();
    try {
      int count = ((Long) em.createQuery("select count(o) from Setting as o" + query.asWhereClause("o")).
        getSingleResult()).intValue();
      return count;
    } finally {
      em.close();
    }
  }

}
