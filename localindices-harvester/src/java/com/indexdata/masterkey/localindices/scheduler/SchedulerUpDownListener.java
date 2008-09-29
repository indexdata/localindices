/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.util.TextUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.log(Level.INFO, "SchedulerUpDownListener: harvester context is initialized...");
        ServletContext ctx = servletContextEvent.getServletContext();
        
        unpackDir(ctx, "/WEB-INF/stylesheets", 
                ctx.getInitParameter("HARVEST_DIR") + "/stylesheets");
        unpackDir(ctx, "/WEB-INF/zebra_dom_conf", 
                ctx.getInitParameter("HARVEST_DIR"));        
        //copyZebraConf(ctx);
        unpackResourceWithSubstitute(ctx, "/WEB-INF/zebra.cfg", 
                ctx.getInitParameter("HARVEST_DIR"), "HARVEST_DIR");
        
        startZebraSrv(ctx);

        st = new SchedulerThread(getInitParamsAsMap(ctx));
        th = new Thread(st);
        th.start();
        ctx.setAttribute("schedulerThread", st);

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
    
    private void unpackResourceWithSubstitute(ServletContext ctx, String source, String dest, String token) {
        File destFile = new File(dest);
        if (!destFile.exists())
            destFile.mkdirs();
        String fileName = source.substring(source.lastIndexOf("/"));
        try {
            InputStream is = ctx.getResourceAsStream(source);
            FileOutputStream os = new FileOutputStream(dest + fileName);
            TextUtils.copyStreamWithReplace(is, os, token, ctx.getInitParameter(token));
            os.close();
            is.close();
        } catch (IOException ioe) {
            logger.log(Level.WARN, "Cannot unpack resource " + source + " to " + dest);
        }
    }
    
    /**
     * 
     * @param ctx
     * @param source full source path (includinf file name)
     * @param dest full destination path (without the file name)
     */
    private void unpackDir(ServletContext ctx, String source, String dest) {
        //first check if the target directory exists, if not create it
        File destDir = new File(dest);
        if (!destDir.exists())
            destDir.mkdirs();
        // get all subfiles from the source and copy them over
        for(Object resource : ctx.getResourcePaths(source)) {
            String resourcePath = (String) resource;
            try {
                InputStream is = ctx.getResourceAsStream(resourcePath);
                String fileName = resourcePath.substring(resourcePath.lastIndexOf("/"));
                FileOutputStream os = new FileOutputStream(dest + "/" + fileName);
                TextUtils.copyStream(is, os);
                os.close();
                is.close();
            } catch (IOException ioe) {
                logger.log(Level.WARN, "Cannot unpack file " + resourcePath + " to " + dest);
            }            
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

