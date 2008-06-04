/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple, single file storage.
 * @author jakub
 */
public class SingleFileStorage implements HarvestStorage {
    private String outFileName;
    private OutputStream fos;
    
    public SingleFileStorage(Harvestable harvestable) {
        this(harvestable.getId() + "-" + harvestable.getName());
    }
    
    public SingleFileStorage(String outFileName) {
        this.outFileName = outFileName;
    }
    
    public void begin() throws IOException {
        fos = new FileOutputStream(outFileName, true);
    }
    
    public void commit() throws IOException {
        fos.close();
    }
    
    public void rollback() throws IOException {
        fos.close();
        File f = new File(outFileName);
        f.delete();
    }

    public void purge() throws IOException {
        this.rollback();
    }
    
    public OutputStream getOutputStream() {
        return fos;
    }
    
    public String getOutFileName() {
        return outFileName;
    }
}
