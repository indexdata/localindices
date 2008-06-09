/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.util.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This storage uses the MultiFileStorage harvested files 
 * and calls zebraidx process on them to do the indexing.
 * 
 * @author jakub
 */
public class ZebraFileStorage extends MultiFileStorage {
    
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");
    private String databaseName;
    private String config;
    
    public ZebraFileStorage(String storageDir, Harvestable harvestable) {
        super(storageDir, harvestable);
        databaseName = "job" + harvestable.getId();
        config = storageDir + "/zebra.cfg";
        // fix that - probaly a open function required
        /*
        try {
            create();
        } catch (IOException ioe) {
            logger.log(Level.INFO, "cannot create db.");
        }
        */
    }
    
    @Override
    public void commit() throws IOException {
        super.commit();
        update();
    }
    
    @Override
    public void purge() throws IOException {
        super.purge();
        drop();
    }

    private void create() throws IOException {
        logger.log(Level.INFO, "Zebra: Creating db: " + databaseName + "...");
        //String[] createCmd = {"zebraidx", "create", databaseName};
        String[] createCmd = {"zebraidx", "-c", config, "init"};
        ProcessUtils.logError(ProcessUtils.execAndReturn(createCmd), logger);
        logger.log(Level.INFO, "Zebra: db created.");    
    }
    
    private void update() throws IOException {        
        logger.log(Level.INFO, "Zebra: updating with records from " + committedDir);
        String[] indexCmd = {"zebraidx", "-c", config, "-d" , databaseName,
                            "update", committedDir};
        ProcessUtils.logError(ProcessUtils.execAndReturn(indexCmd), logger);
        
        logger.log(Level.INFO, "Zebra: update complete.");
        
        String[] commitCmd = {"zebraidx", "-c", config, "commit"};
        ProcessUtils.logError(ProcessUtils.execAndReturn(commitCmd), logger);
        
        logger.log(Level.INFO, "Zebra: data committed.");
    }
    
    private void drop() throws IOException {
        logger.log(Level.INFO, "Zebra: droping the db: " + databaseName);
        String[] dropCmd = {"zebraidx", "-c", config, "drop", databaseName};
        ProcessUtils.logError(ProcessUtils.execAndReturn(dropCmd), logger);
        logger.log(Level.INFO, "Zebra: db dropped.");
    }
    
}
