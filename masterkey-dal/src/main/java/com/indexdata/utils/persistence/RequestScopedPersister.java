/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.utils.persistence;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 *
 * @author jakub
 */
public class RequestScopedPersister implements ServletRequestListener, ServletContextListener {

    @Override
    public void requestInitialized(ServletRequestEvent e) {
        //do nothing here EntityManager is lazy loaded
    }

    @Override
    public void requestDestroyed(ServletRequestEvent e) {
        EntityUtil.closeManager();
    }

    @Override
    public void contextInitialized(ServletContextEvent e) {
        EntityUtil.initialize("localindicesPU");
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        EntityUtil.terminate();
    }

}
