/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.util.*;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This storage uses the MultiFileStorage harvested files 
 * and calls zebraidx process on them to do the indexing.
 * 
 * @author jakub
 */
public class ZebraFileStorage extends MultiFileStorage {
    
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private String databaseName;
    private String config;
    private String domConf;
    
    public ZebraFileStorage(String storageDir, Harvestable harvestable, String domConf) {
        super(storageDir, harvestable);
        databaseName = "job" + harvestable.getId();
        config = storageDir + "/zebra.cfg";
        this.domConf = "dom." + storageDir + "/" + domConf;
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
    
    private void update() throws IOException {        
        logger.log(Level.INFO, "Zebra: updating with records from " + committedDir);
        String[] indexCmd = {"zebraidx", "-c", config, "-t", domConf, "-d" , databaseName,
                            "update", committedDir};
        int ret = ProcessUtils.execAndWait(indexCmd, logger);
        if (ret != 0) throw new IOException("Updating DB failed.");
        
        logger.log(Level.INFO, "Zebra: update complete.");
        
        String[] commitCmd = {"zebraidx", "-c", config, "-t", domConf, "commit"};
        ret = ProcessUtils.execAndWait(commitCmd, logger);
        if (ret != 0) throw new IOException("Commit when updating failed.");        
        
        logger.log(Level.INFO, "Zebra: data committed.");
    }
    
    private void drop() throws IOException {
        logger.log(Level.INFO, "Zebra: dropping the db: " + databaseName);
        String[] dropCmd = {"zebraidx", "-c", config, "-t", domConf, "drop", databaseName};
        int ret = ProcessUtils.execAndWait(dropCmd, logger);
        if (ret != 0) throw new IOException("Dropping DB failed.");        
        logger.log(Level.INFO, "Zebra: db dropped.");
        String[] commitCmd = {"zebraidx", "-c", config, "-t", domConf, "commit"};
        ret = ProcessUtils.execAndWait(commitCmd, logger);        
        if (ret != 0) throw new IOException("Commit when dropping DB failed.");        
        logger.log(Level.INFO, "Zebra: data committed.");
    }
    
}
