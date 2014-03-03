/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class stores the harvested resources onto the file system and indexes them with Zebra
 * 
 * Directory structure
 *    /basepath
 *       /committed
 *           /job-NNN
 *              file-DDDD 
 *       /incoming
 *           /job-NNN
 *              file-DDDD
 * The jobs are harvested into the incoming directory, and when committed, are moved
 * to the committed directory.
 * 
 * @author Heikki
 */
public class MultiFileStorage implements RecordStorage {
  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  protected String basePath; // where the data is to be stored
  protected String incomingDir; // dir (under basePath) for this job
  protected String committedDir; // dir for committed harvests
  protected String currentFileName; // the file name, no path
  private String namePrefix; // file name prefix (in jobDir)
  private OutputStream fos; // the "file handle"
  private boolean overwriteMode = false; // default to appending, as before
  @SuppressWarnings("unused")
  private Harvestable harvestable;

  public MultiFileStorage(String storageDir, Harvestable harvestable) {
    basePath = storageDir;
    incomingDir = basePath + "/incoming" + "/job" + harvestable.getId();
    committedDir = basePath + "/committed" + "/job" + harvestable.getId();
    namePrefix = harvestable.getId().toString();

    logger.log(Level.INFO, "File storage " + "incoming: '" + incomingDir + "' " + "commited: '"
	+ committedDir + "'");
  }

  /**
   * Check if the incoming directory exists, and if not, create it. Actually,
   * delete the old one if it exists, and create a new one. That way, we roll
   * back what ever old cruft we may have had in the incoming directory
   * 
   * @throws java.io.IOException
   */
  private void checkIncomingDir() throws IOException {
    File f = new File(incomingDir);
    if (f.exists()) { // old uncommitted stuff, roll back

      logger.log(Level.INFO, "Rolling back old incoming directory '" + f.getPath() + "'");
      for (File ff : f.listFiles()) {
	if (!ff.delete()) {
	  throw new IOException("Could not delete old file '" + ff.getPath() + "'");
	}
      }
      if (!f.delete()) {
	throw new IOException("Could not delete old file '" + f.getPath() + "'");
      }
      // create the incoming dir
    }
    if (!f.mkdirs()) {
      throw (new IOException("Could not create incoming " + "job direcotry '" + incomingDir + "'"));
    }
  } // check JobDir

  /**
   * Open a new putput file in the incoming directory Checks that the directory
   * exists, creates if necessary
   * 
   * @throws java.io.IOException
   */
  public void begin() throws IOException {
    checkIncomingDir();
    String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    currentFileName = "/" + namePrefix + "-" + timeStamp;
    fos = new FileOutputStream(incomingDir + "/" + currentFileName, true);
  }

  /**
   * Closes and commits the output file Makes sure the committed directory
   * exists Moves the harvested file into the committed dir Removes the
   * directory from incoming
   * 
   * @throws java.io.IOException
   */
  public void commit() throws IOException {
    fos.close();
    File fc = new File(committedDir);
    if (overwriteMode && fc.exists()) {
      deleteDir(fc);
    }
    fc = new File(committedDir);
    if (!fc.exists()) {
      if (!fc.mkdirs()) {
	throw new IOException("Could not create commit-dir '" + fc.getPath() + "'");
      }
    }
    File fi = new File(incomingDir + "/" + currentFileName);
    if (!fi.renameTo(new File(committedDir + "/" + currentFileName))) {
      throw new IOException("Could not commit harvested file '" + fi.getPath() + "'");
    }
    fi = new File(incomingDir);
    if (!fi.delete()) {
      throw new IOException("Could not delete incoming dir'" + fi.getPath() + "'");
    }
    logger.log(Level.INFO, "Committed dir '" + fc.getPath() + "'");

  } // commit

  /**
   * Recursively deletes the given directory
   * 
   * @param f
   *          the directory (or file) to be deleted
   * @throws java.io.IOException
   */
  private void deleteDir(File f) throws IOException {
    if (!f.exists())
      return;
    logger.log(Level.DEBUG, "Recursing into '" + f.getCanonicalPath() + "'" + " for deleting it");
    for (File ff : f.listFiles()) {
      if (ff.isDirectory()) {
	deleteDir(ff);
      }
      if (!ff.delete()) {
	throw new IOException("Could not delete '" + ff.getPath() + "'");
      }
    }
    if (!f.delete()) {
      throw new IOException("Could not delete '" + f.getPath() + "'");
    }
  } // deleteDir

  /**
   * Remove all that we have on this job
   * 
   * @throws java.io.IOException
   */
  public void purge(boolean commit) throws IOException {
    logger.log(Level.INFO, "Purge '" + basePath + "'");
    deleteDir(new File(committedDir));
    deleteDir(new File(incomingDir));
    if (!commit) {
      logger.log(Level.WARN, "Purge is always committed on MultiFileStorage '" + basePath + "'");
    }
  }

  /**
   * Close the output and remove the current file
   * 
   * @throws java.io.IOException
   */
  public void rollback() throws IOException {
    fos.close();
    File fi = new File(incomingDir + "/" + currentFileName);
    if (fi.exists()) {
      if (!fi.delete()) {
	throw new IOException("Could not delete harvested file '" + fi.getPath() + "'");
      }
    }
    // clean mess from previous commits
    fi = new File(incomingDir);
    if (fi.exists()) {
      if (!fi.delete()) {
	throw new IOException("Could not delete incoming dir'" + fi.getPath() + "'");
      }
    }
    // cleanup committed file if commit gotten that far
    File fc = new File(committedDir + "/" + currentFileName);
    if (fc.exists()) {
      if (!fc.delete()) {
	throw new IOException("Could not delete harvested file '" + fc.getPath() + "'");
      }
    }
  } // rollback

  public OutputStream getOutputStream() {
    return fos;
  }

  /* Probably useless. Remove some day! */
  public String getOutFileName() {
    return incomingDir + "/" + currentFileName;
  }

  public void setOverwriteMode(boolean mode) {
    overwriteMode = mode;
  }

  public boolean getOverwriteMode() {
    return overwriteMode;
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void databaseEnd() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void add(Record record) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Record get(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void shutdown() throws IOException {
    // TODO Auto-generated method stub
  }

} // MultiFileStorage
