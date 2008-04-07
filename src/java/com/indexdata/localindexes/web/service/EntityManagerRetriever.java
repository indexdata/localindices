/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.service;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

/**
 *
 * @author jakub
 */
public class EntityManagerRetriever {
    private static String persistenceUnitName = "localindexes";
           
    public static EntityManager getEntityManager() {
        return Persistence.createEntityManagerFactory(persistenceUnitName).createEntityManager();
    }
}
