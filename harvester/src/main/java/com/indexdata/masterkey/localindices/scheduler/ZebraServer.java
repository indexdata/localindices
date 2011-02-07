/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.util.ProcessUtils;

/**
 * Wrapper class around zebrasrv.
 * @author jakub
 */
public class ZebraServer implements Runnable {
    private String config;
    private int portNum;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    
    public ZebraServer(String storageDir, int portNum) {
        config = storageDir + "/zebra.cfg"; 
        this.portNum = portNum;
    }

    public void run() {
        try {
            String[] cmd = {"zebrasrv", "-c", config,  "@:" + portNum};
            ProcessUtils.execAndWait(cmd, logger);
        } catch (IOException ex) {
            logger.log(Level.ERROR, "ZebraServer: failure in zebrasrv process.", ex);
        }
    }
}
