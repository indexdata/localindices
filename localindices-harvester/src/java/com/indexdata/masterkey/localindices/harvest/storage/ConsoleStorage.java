/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple console storage that prints out the harvested data.
 * @author jakub
 */
public class ConsoleStorage implements HarvestStorage {

    public void begin() throws IOException {
        System.out.println("--- Storage write begun ---");
    }

    public void commit() throws IOException {
        System.out.println("--- Storage write commited ---");
    }
    
    public void rollback() throws IOException {
        System.out.println("--- Storage write rolled back, last write discarded ---");    
    }
        
    public void purge() throws IOException {
        System.out.println("--- Storage write purged, all previous write discarded ---");    
    }
    
    public OutputStream getOutputStream() {
        return System.out;
    }
}
