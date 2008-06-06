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
 * @author heikki
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

    private void copyZebraConf(ServletContext servletContext) {
        String source = "/WEB-INF/zebra.cfg";
        String dest = servletContext.getInitParameter("HARVEST_DIR") + "/zebra.cfg";
        try {
            copyResource(servletContext, source, dest);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Cannot copy required resource " + source + " to " + dest);
        }
    }
    
    private void copyResource(ServletContext ctx, String relPath, String absPath) throws IOException {
        InputStream is = ctx.getResourceAsStream(relPath);
        FileOutputStream os = new FileOutputStream(absPath);
        TextUtils.copyStreamWithReplace(is, os, "HARVEST_DIR", ctx.getInitParameter("HARVEST_DIR"));
        os.close();
    }

    private Map<String, String> getInitParamsAsMap(ServletContext ctx) {
        Map<String, String> paramMap = new HashMap<String, String>();
        Enumeration paramNames = ctx.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName =  (String) paramNames.nextElement();
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

