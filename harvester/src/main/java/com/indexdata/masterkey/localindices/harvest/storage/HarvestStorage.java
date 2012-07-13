/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Harvest storage API.
 * 
 * @author jakub
 */
public interface HarvestStorage {

  /**
   * Opens the storage and prepares output stream for writes.
   * 
   * @throws java.io.IOException
   */
  public void begin() throws IOException;

  /**
   * Commits the current harvest and closes output stream.
   * 
   * @throws java.io.IOException
   */
  public void commit() throws IOException;

  /**
   * Rolls back the current write and closes the output stream.
   * 
   * @throws java.io.IOException
   */
  public void rollback() throws IOException;

  /**
   * Purges all data written so far (drops the whole storage).
   * 
   * @throws java.io.IOException
   */
  public void purge(boolean commit) throws IOException;

  //public void purgeCommit() throws IOException;

  
  /**
   * Set/get a flag that indicates the overwrite mode Normally a storage is in
   * append mode, so new transactions (begin-write-commit) are appended to the
   * existing ones. But in overwrite mode, we remove all the old stuff when
   * doing the commit. Useful for things that need to be harvested all over
   * again, like bulk uploads.
   */
  public void setOverwriteMode(boolean mode);

  public boolean getOverwriteMode();

  /**
   * Returns an output stream that allows for writing data to the storage.
   * 
   * @return the output stream for writing to it
   */
  public OutputStream getOutputStream();
}
