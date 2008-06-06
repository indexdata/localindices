/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class around zebrasrv.
 * @author jakub
 */
public class ZebraServer implements Runnable {
    private String config;
    private int portNum;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey2.localindices");
    
    public ZebraServer(String storageDir, int portNum) {
        config = storageDir + "/zebra.cfg"; 
        this.portNum = portNum;
    }

    public void run() {
        try {
            String[] cmd = {"zebrasrv", "-c", config, "@:" + portNum};
            ProcessUtils.execAndWait(cmd, logger);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "ZebraServer: failure in zebrasrv process.", ex);
        }
    }
}
