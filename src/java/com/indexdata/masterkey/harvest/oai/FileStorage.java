package com.indexdata.masterkey.harvest.oai;

import com.indexdata.localindexes.web.entity.Harvestable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 *
 * @author jakub
 */
public class FileStorage implements HarvestStorage {
    private String outFileName;
    private OutputStream fos;
    
    public FileStorage(Harvestable harvestable) throws FileNotFoundException {
        this(harvestable.getName() + harvestable.getId());
    }
    
    public FileStorage(String outFileName) throws FileNotFoundException {
        this.outFileName = outFileName;
        fos = new FileOutputStream(outFileName, true);
    }

    public OutputStream getOutputStream() {
        return fos;
    }
    
    public String getOutFileName() {
        return outFileName;
    }

}
