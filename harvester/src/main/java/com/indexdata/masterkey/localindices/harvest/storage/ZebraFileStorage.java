/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.util.*;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This storage uses the MultiFileStorage harvested files and calls zebraidx
 * process on them to do the indexing.
 * 
 * @author jakub
 */
public class ZebraFileStorage extends MultiFileStorage {

  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  private String databaseName;
  private String config;
  private String domConf;

  private void ensureDir(String storageDir, String dir) {
    File f = new File(storageDir + "/" + dir);
    if (!f.exists()) {
      f.mkdirs();
      logger.log(Level.INFO, "Created dir " + "'" + storageDir + "/" + dir + "'");
    }

  }

  public ZebraFileStorage(String storageDir, Harvestable harvestable, String domConf) {
    super(storageDir, harvestable);
    databaseName = "job" + harvestable.getId();
    config = storageDir + "/zebra.cfg";
    this.domConf = "dom." + storageDir + "/" + domConf;
    ensureDir(storageDir, "reg");
    ensureDir(storageDir, "shadow");
    ensureDir(storageDir, "tmp");
    ensureDir(storageDir, "lock");
  }

  @Override
  public void commit() throws IOException {
    super.commit();
    if (super.getOverwriteMode()) {
      try {
	zebraDrop();
      } catch (IOException ex) {
	logger.log(Level.INFO, "Dropping the database failed: " + ex.getMessage());
	// Most likely it failed because we didn't have one already,
	// so we ignore the error here. If the disk is broken, the
	// next operation(s) will fail too!
      }
    }
    zebraUpdate();
    zebraCommit();
  }

  @Override
  public void purge() throws IOException {
    super.purge();
    zebraDrop();
    zebraCommit();
  }

  private void zebraUpdate() throws IOException {
    logger.log(Level.INFO, "Zebra: updating with records from " + committedDir);

    String[] indexCmd = { "zebraidx", "-c", config, "-t", domConf, "-d", databaseName, "update",
	committedDir };
    int ret = ProcessUtils.execAndWait(indexCmd, logger);
    if (ret != 0) {
      throw new IOException("Updating DB failed. rc=" + ret);
    }
    logger.log(Level.INFO, "Zebra: update complete.");
  }

  private void zebraDrop() throws IOException {
    logger.log(Level.DEBUG, "Zebra: dropping the db: " + databaseName);
    String[] dropCmd = { "zebraidx", "-c", config, "-t", domConf, "drop", databaseName };
    int ret = ProcessUtils.execAndWait(dropCmd, logger);
    if (ret != 0) {
      throw new IOException("Dropping DB failed. rc=" + ret);
    }
    logger.log(Level.INFO, "Zebra: db dropped.");

  }

  private void zebraCommit() throws IOException {
    String[] commitCmd = { "zebraidx", "-c", config, "-t", domConf, "commit" };
    int ret = ProcessUtils.execAndWait(commitCmd, logger);
    if (ret != 0) {
      throw new IOException("Commit when dropping DB failed. rc=" + ret);
    }
    logger.log(Level.INFO, "Zebra: data committed.");

  }
}
