/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.utils.persistence;

import javax.persistence.Persistence;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

/**
 *
 * @author jakub
 */

/*
* ===Persistence stuff===
*
* Primarily, there are two ways of doing persistence:
* - container-managed persistence (in so-called JEE environment)
* - bean-managed persistence (or application-managed) (in JSE env)
*
* CMT:
* + may use JTA transactions (UserTransaction API and EM.joinTX) or resource-local
*   (EntityTransaction) - this is specfied in persistence.xml
* + uses injected (through @PersistenceContext) or looked-up EntityManager
* + if using JTA UserTransaction is also injected (through @Resource) or looked-up
* + as enity manager is lifetime is controlled by the server it's nerver
*   closed in the finally block
*
* Examples:
*
* @PersistenceContext(name = "persistence/localindicesPU", unitName = "localindicesPU")
*
*    private EntityManager getEntityManager() {
*        EntityManager em = null;
*        try {
*            em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/localindicesPU");
*        } catch (NamingException e) {
*            logger.log(Level.DEBUG, e);
*        }
*        return em;
*    }
*
*    private UserTransaction getUserTransaction() {
*        UserTransaction utx = null;
*        try {
*            utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
*        } catch (NamingException e) {
*            logger.log(Level.DEBUG, e);
*        }
*        return utx;
*    }
*
* BMT:
* + it's possible to use JTA, but it does not do anything under the hood
*    so we stick with EntityTransactions (resource-local)
* + in theory we can inject EntityManagerFactory, but that does not seem to work
*   (at least on not on Tomcat) so we get a hold of it through
*   Persistence.createEntityManagerFactory(PU)
* + EMF is thread-safe but EM is not if we want to share it we need to have
*   a per-thread copy (ThreadLocal)
* + remember: EM has to be closed in the finally block or after request (filter)
*
* Examples:
*
* @PersistenceUnit(unitName = "localindicesPU")
* private EntityManagerFactory emf;
*
* private EntityManager getEntityManager() {
*     return emf.createEntityManager();
* }
*/

public class EntityUtil {
    private static EntityManagerFactory emf;
    private static ThreadLocal<EntityManager> perThread;

    /**
     * This method creates underlying EntityManagerFactory and should be called
     * before any other action takes place (e.g at the begining of the application
     * deployment). Further calls are no-ops.
     * @param unitName
     */
    static synchronized void initialize (String unitName) {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory(unitName);
            perThread = new ThreadLocal<EntityManager>();
        }
    }

    /**
     * This method closes underlying EntityManagerFactory instance, and allows
     * for new initialization. It should be called at the end of the appplication
     * lifecycle.
     */
    static synchronized void terminate () {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
            perThread = null;
        }
    }

    /**
     * Returns an entity manager associated with a current thread. It is usually used in
     * combination with a servltet filter or listener and the manager is automatically
     * closed at the end of the request.
     * @return entity manager associated with the current thread
     */
    public static EntityManager getManager() {
        if (emf == null) throw new IllegalStateException("EntitityUtil not initialized.");
        EntityManager manager = perThread.get();
        if (manager == null || !manager.isOpen()) {
            manager = emf.createEntityManager();
            perThread.set(manager);
        }
        return manager;
    }

    /**
     * Closes EntitymManager associated with current thread (if any).
     */
    public static void closeManager() {
        if (emf == null) throw new IllegalStateException("EntitityUtil not initialized.");
        EntityManager manager = perThread.get();
        if (manager != null) {
            if (manager.isOpen()) manager.close();
            perThread.set(null);
        }
    }
}