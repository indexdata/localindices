package com.indexdata.masterkey.harvest.oai;

import com.indexdata.localindexes.web.entity.Harvestable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author jakub
 */
public class FileStorage implements HarvestStorage {
    private String outFileName;
    private OutputStream fos;
    
    public FileStorage(Harvestable harvestable) {
        this(harvestable.getName() + harvestable.getId());
    }
    
    public FileStorage(String outFileName) {
        this.outFileName = outFileName;
    }
    
    public void openOutput() throws IOException {
        fos = new FileOutputStream(outFileName, true);
    }
    
    public void closeOutput() throws IOException {
        fos.close();
    }

    public OutputStream getOutputStream() {
        return fos;
    }
    
    public String getOutFileName() {
        return outFileName;
    }

}
