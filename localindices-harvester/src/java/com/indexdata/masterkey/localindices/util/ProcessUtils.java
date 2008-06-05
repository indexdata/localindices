/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process helper class.
 * @author jakub
 */
public class ProcessUtils {
     private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.util.ProcessUtils");

     public static Process exec(String[] cmd) throws IOException {
        Process proc = Runtime.getRuntime().exec(cmd);    
        //InputStream is = proc.getInputStream();
        InputStream is = proc.getErrorStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            logger.log(Level.INFO, line);
        }
        return proc;
    }
     public static int execAndWait(String[] cmd) throws IOException, InterruptedException {
         return exec(cmd).waitFor();
     }  
}
