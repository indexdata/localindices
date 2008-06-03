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
 * @author jakub
 */
public interface HarvestStorage {
    
    /** 
     * Opens the storage and prepares output stream for writes.
     * @throws java.io.IOException
     */
    public void begin() throws IOException;
    
    /**
     * Commits the current harvest and closes output stream.
     * @throws java.io.IOException
     */
    public void commit() throws IOException;
    
    /**
     * Rolls back the current write and closes the output stream.
     * @throws java.io.IOException
     */      
    public void rollback() throws IOException;
    
    /**
     * Purges all data written so far (drops the whole storage).
     * @throws java.io.IOException
     */
    public void purge() throws IOException;
    
    /** 
     * Returns an output stream that allows for writing data to the storage.
     * @return the output stream for writing to it
     */
    public OutputStream getOutputStream();    
}
