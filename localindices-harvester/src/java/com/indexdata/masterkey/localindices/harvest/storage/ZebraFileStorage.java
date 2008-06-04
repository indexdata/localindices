/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
public class ZebraFileStorage extends MultiFileStorage {
    
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");
    
    public ZebraFileStorage(String storageDir, Harvestable harvestable) {
        super(storageDir, harvestable);
    }
    
    public void commit() throws IOException {
        super.commit();
        execZebra();
    }
    
    private void execZebra() throws IOException {
        
        logger.log(Level.INFO, "Zebra indexer started.");

        String[] zebraCmd = {"ls", committedDir};
        Process zebraProc = Runtime.getRuntime().exec(zebraCmd);
        
        InputStream is = zebraProc.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            logger.log(Level.INFO, line);
        }
        logger.log(Level.INFO, "Zebra indexer has finshed.");
    }
}
