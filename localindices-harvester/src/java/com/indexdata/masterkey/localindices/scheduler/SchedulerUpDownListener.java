/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.util.TextUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Context listener for the scheduler application.
 * Starts the scheduling thread when the the application is deployed,
 * and kills it when the server is going down. Places the schedulerThread 
 * in application context.
 * 
 * @author jakub
 */
public class SchedulerUpDownListener implements ServletContextListener {

    private Thread th;
    private SchedulerThread st;
    private Thread zsrvT;
    private ZebraServer zsrv;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.log(Level.INFO, "SchedulerUpDownListener: harvester context is initialized...");
        ServletContext servletContext = servletContextEvent.getServletContext();

        copyZebraConf(servletContext);
        copyDomXmlConf(servletContext);
        copyXsltStylesheets(servletContext);
        startZebraSrv(servletContext);

        st = new SchedulerThread(getInitParamsAsMap(servletContext));
        th = new Thread(st);
        th.start();
        servletContext.setAttribute("schedulerThread", st);

        logger.log(Level.INFO, "SchedulerUpDownListener: scheduling thread created, started and placed in the context.");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (st != null) {
            logger.log(Level.INFO, "SchedulerUpDownListener: telling the scheduling thread to stop...");
            st.kill();
            logger.log(Level.INFO, "SchedulerUpDownListener: waking the scheduling thread up so it can close down...");
            th.interrupt();
        }
        if (zsrvT != null) {
            logger.log(Level.INFO, "SchedulerUpDownListener: shuting down zserv...");
            zsrvT.interrupt();
        }
        logger.log(Level.INFO, "SchedulerUpDownListener: application context destroyed.");
    }

    private void copyXsltStylesheets(ServletContext ctx) {
        String source = "/WEB-INF/dcaddmeta.xsl";
        String dest = ctx.getInitParameter("HARVEST_DIR") + "/dcaddmeta.xsl";
        try {
            InputStream is = ctx.getResourceAsStream(source);
            FileOutputStream os = new FileOutputStream(dest);
            TextUtils.copyStream(is,os);
            os.close();
            is.close();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Cannot copy required resource " + source + " to " + dest);
        }    
    }

    private void copyZebraConf(ServletContext ctx) {
        String source = "/WEB-INF/zebra.cfg";
        String dest = ctx.getInitParameter("HARVEST_DIR") + "/zebra.cfg";
        try {
            InputStream is = ctx.getResourceAsStream(source);
            FileOutputStream os = new FileOutputStream(dest);
            TextUtils.copyStreamWithReplace(is, os, "HARVEST_DIR", ctx.getInitParameter("HARVEST_DIR"));
            os.close();
            is.close();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Cannot copy required resource " + source + " to " + dest);
        }
    }

    private void copyDomXmlConf(ServletContext ctx) {
        String source = "/WEB-INF/dom-conf.xml";
        String dest = ctx.getInitParameter("HARVEST_DIR") + "/dom-conf.xml";
        try {
            InputStream is = ctx.getResourceAsStream(source);
            FileOutputStream os = new FileOutputStream(dest);
            TextUtils.copyStream(is,os);
            os.close();
            is.close();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Cannot copy required resource " + source + " to " + dest);
        }
    }

    private Map<String, String> getInitParamsAsMap(ServletContext ctx) {
        Map<String, String> paramMap = new HashMap<String, String>();
        Enumeration paramNames = ctx.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            paramMap.put(paramName, ctx.getInitParameter(paramName));
        }
        return paramMap;
    }

    private void startZebraSrv(ServletContext ctx) {
        int portNum = Integer.parseInt(ctx.getInitParameter("ZEBRASRV_PORT"));
        logger.log(Level.INFO, "Starting zebrasrv at port " + portNum);
        zsrv = new ZebraServer(ctx.getInitParameter("HARVEST_DIR"), portNum);
        zsrvT = new Thread(zsrv);
        zsrvT.start();
    }
}

