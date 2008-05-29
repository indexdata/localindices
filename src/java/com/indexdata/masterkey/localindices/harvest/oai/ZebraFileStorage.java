/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 * 
 * 
 */

package com.indexdata.masterkey.localindices.harvest.oai;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Formatter;

/**
 *
 * @author Jakub and Heikki
 */
public class ZebraFileStorage implements HarvestStorage {
    private String outFileName;
    private String baseName;
    private OutputStream fos;
    
    public ZebraFileStorage(Harvestable harvestable) {
        this(  harvestable.getId() + "." + harvestable.getName() );
    }
    
    public ZebraFileStorage(String outFileName) {
        this.outFileName = outFileName;
        // FIXME - get the path to the file storage from somewhere
        baseName=outFileName; // just in case someone asks for it before opening
    }
    
    private String timeStamp() {
        Calendar g = new GregorianCalendar(); // defaults to now()
        int sec = g.get(Calendar.SECOND);
        int min = g.get(Calendar.MINUTE);
        int hour = g.get(Calendar.HOUR_OF_DAY);
        int mday = g.get(Calendar.DAY_OF_MONTH);
        int mon = g.get(Calendar.MONTH) + 1;  // JAN = 1
        int year = g.get(Calendar.YEAR);
        Formatter f = new Formatter();
        f.format("%04d%02d%02d-%02d%02d%02d", year,mon,mday, hour, min, sec);
        return f.toString();
    }
            
    
    public void openOutput() throws IOException {
        outFileName = baseName + timeStamp();
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

    public void removeAll() throws IOException {
        
    }
    public void closeAndDelete() throws IOException {
        fos.close();
        File f = new File(outFileName);
        f.delete();
    }
}
