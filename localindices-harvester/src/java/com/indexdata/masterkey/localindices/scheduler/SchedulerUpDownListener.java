/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.util.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
        logger.log(Level.INFO, "Harvester context is being initialized...");
        ServletContext ctx = servletContextEvent.getServletContext();        
        //load default, fallback settings
        Properties props = null;
        try {
            props = loadDefaults(ctx);
        } catch (IOException ex) {
            logger.log(Level.FATAL, "Default harvester properties missing from the archive, deployment aborted.");
            return;
        }
        //override with user settings, if any otherwise try to unpack the defaults
        String configPath = ctx.getInitParameter("USER_CONFIG_PATH");
        if (configPath == null) {
            logger.log(Level.WARN, "CONFIG_PATH not specified, will use only defaults");
        } else {
            try {
                FileInputStream userPropsFis = new FileInputStream(configPath);
                props.load(userPropsFis);
                userPropsFis.close();
            } catch (FileNotFoundException ex) {
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(configPath);
                    props.store(fos, "In order for the canges to cause effect, the harvester needs to be redeployed");
                    fos.close();
                } catch (FileNotFoundException ex2) {
                    //very weird
                    logger.log(Level.WARN, ex2);
                } catch (IOException ioe) {
                    logger.log(Level.WARN, ioe);
                }
            } catch (IOException ioe) {
                logger.log(Level.WARN, ioe);
            }
        }

        //validate harvest dir
        String harvestDirPath = props.getProperty("harvester.dir");
        File harvestDir = new File(harvestDirPath);
        boolean hasDir = true;
        if (!harvestDir.exists()) {
            logger.log(Level.INFO, "HARVEST_DIR does not seem to exist, trying to create...");
            hasDir = harvestDir.mkdir();
        }
        if (!hasDir) {
            logger.log(Level.FATAL, "Cannot access HARVEST_DIR at"
                    + harvestDirPath + ", deployment aborted.");
            return;
        }

        //zebra dirs, configs, etc
        new File(harvestDir, "reg").mkdir();
        new File(harvestDir, "shadow").mkdir();
        new File(harvestDir, "lock").mkdir();
        new File(harvestDir, "tmp").mkdir();        
        unpackDir(ctx, "/WEB-INF/stylesheets", harvestDirPath + "/stylesheets");
        unpackDir(ctx, "/WEB-INF/zebra_dom_conf", harvestDirPath);
        unpackResourceWithSubstitute(ctx, "/WEB-INF/zebra.cfg", harvestDirPath, 
                "HARVEST_DIR", harvestDirPath);
        startZebraSrv(props);

        //load properties to a config
        Map<String,Object> config = new HashMap(props);

        //http proxy settings
        String proxyHost = props.getProperty("harvester.http.proxyHost");
        if (proxyHost != null) {
            int proxyPort = 80;
            try {
                proxyPort = Integer.parseInt(props.getProperty("harvester.http.proxyPort"));
            } catch (NumberFormatException nfe) {
                logger.log(Level.WARN, "Http proxy port is invalid");
            }
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(proxyHost, proxyPort));
                config.put("harvester.http.proxy", proxy);
        }

        //put the config and scheduler in the context
        ctx.setAttribute("harvester.config", config);
        st = new SchedulerThread(config);
        th = new Thread(st);
        th.start();
        ctx.setAttribute("schedulerThread", st);
        logger.log(Level.INFO, "Scheduler created, started and placed in the context.");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {       
        logger.log(Level.INFO, "Harvester is being undeployed...");
        if (st != null) {
            logger.log(Level.INFO, "Stopping the scheduler...");
            st.kill();
            logger.log(Level.INFO, "Interrupting the scheduler...");
            th.interrupt();
        }
        if (zsrvT != null) {
            logger.log(Level.INFO, "Shutting down zserv...");
            zsrvT.interrupt();
        }
        logger.log(Level.INFO, "Harvester context destroyed.");
    }

    private Properties loadDefaults(ServletContext ctx) throws IOException {
        InputStream is = ctx.getResourceAsStream("/WEB-INF/harvester.properties");
        Properties props = new Properties();
        props.load(is);
        return props;
    }
    
    private void unpackResourceWithSubstitute(ServletContext ctx, 
            String source, String dest, String oldToken, String newToken) {
        File destFile = new File(dest);
        if (!destFile.exists())
            destFile.mkdirs();
        String fileName = source.substring(source.lastIndexOf("/"));
        try {
            InputStream is = ctx.getResourceAsStream(source);
            FileOutputStream os = new FileOutputStream(dest + fileName);
            TextUtils.copyStreamWithReplace(is, os, oldToken, newToken);
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

    private void startZebraSrv(Properties prop) {
        int portNum = Integer.parseInt(prop.getProperty("harvester.zebra.port"));
        logger.log(Level.INFO, "Starting zebrasrv at port " + portNum);
        zsrv = new ZebraServer(prop.getProperty("harvester.dir"), portNum);
        zsrvT = new Thread(zsrv);
        zsrvT.start();
    }
}

