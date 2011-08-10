/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.utils.persistence.EntityUtil;


/**
 * Java Persistence API implementation of the DAO
 * @author jakub
 */
public class StoragesDAOJPA implements StorageDAO {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    private EntityManager getEntityManager() {
        return EntityUtil.getManager();
    }
    
    @Override
    public void createStorage(Storage storage) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            storage.setCurrentStatus("NEW");
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
    public Storage retrieveStorageById(Long id) {
        EntityManager em = getEntityManager();
        Storage entity = em.find(Storage.class, id);
        em.close();
        return entity;
    }

    @Override
    public Storage updateStorage(Storage updEntity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        Storage entity = null;
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
    public void deleteStorage(Storage entity) {
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
	@Override
    public List<Storage> retrieveStorages(int start, int max) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<Storage> hables = null;
        try {
            tx.begin();
            Query q = em.createQuery("select object(o) from Storage as o");
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
    public List<StorageBrief> retrieveStorageBriefs(int start, int max) {
        List<StorageBrief> hrefs = new ArrayList<StorageBrief>();
        for (Storage hable : retrieveStorages(start, max)) {
            StorageBrief href = new StorageBrief(hable);
            hrefs.add(href);
        }
        return hrefs;
    }

    @Override
    public Storage retrieveFromBrief(StorageBrief href) {
        return retrieveStorageById(href.getId());
    }

    @Override
    public int getStorageCount() {
        EntityManager em = getEntityManager();
        try {
            int count = ((Long) em.createQuery("select count(o) from Storage as o").getSingleResult()).intValue();
            return count;
        } finally {
            em.close();
        }    
    }


    @Override
    public InputStream getStorageLog(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
