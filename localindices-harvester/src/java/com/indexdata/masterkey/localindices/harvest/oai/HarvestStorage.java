/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

import java.io.IOException;
import java.io.OutputStream;

public interface HarvestStorage {
    
    /** 
     * Open the storage
     * @throws java.io.IOException
     */
    public void begin() throws IOException;
    
    /**
     * Commits the current harvest
     * Closes storage, etc
     * @throws java.io.IOException
     */
    public void commit() throws IOException;
    
    /**
     * Rolls back the current harvest, but keeps the older ones
     * Closes the storage, etc
     * @throws java.io.IOException
     */
            
    public void rollback() throws IOException;
    
    /**
     * Purges all stuff we have for this storage
     * Deletes files, directories, etc. Tells zebra to forget it, or what ever
     * @throws java.io.IOException
     */
    public void purge() throws IOException;
    
    /** 
     * 
     * @return the output stream for writing to it
     */
    public OutputStream getOutputStream();    
}
