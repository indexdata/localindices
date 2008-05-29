/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 * 
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Heikki
 */
public class ZebraFileStorage implements HarvestStorage {

    private String basePath;   // where the data is to be stored

    private String incomingDir;  // dir (under basePath) for this job

    private String committedDir; // dir for committed harvests

    private String namePrefix; // file name prefix (in jobDir)

    private String currentFileName; // the file name, no path

    private String outFileName;// Actual complete file we are writing in
    private OutputStream fos;  // the "file handle"

    private static Logger logger;

    public ZebraFileStorage(String storageDir,
            Harvestable harvestable,
            Logger alogger) {
        logger = alogger;
        basePath = storageDir;
        incomingDir = basePath + "/incoming" + "/job" + harvestable.getId();
        committedDir = basePath + "/committed" + "/job" + harvestable.getId();
        namePrefix = harvestable.getId().toString();
        outFileName = ""; // not opened yet

        fos = null;
        logger.log(Level.FINER, "ZebraFileStorage: " +
                "i='" + incomingDir + "' " +
                "c='" + committedDir + "'");
    }

    /** Check if the incoming directory exists, and if not, create it.
     * Actually, delete the old one if it exists, and create a new one.
     * That way, we roll back what ever old cruft we may have had in the
     * incoming directory
     * @throws java.io.IOException
     */
    private void checkIncomingDir() throws IOException {
        File f = new File(incomingDir);
        if (f.exists()) {  // old uncommitted stuff, roll back
            logger.log(Level.INFO,"Rolling back old incoming directory '"+
                    f.getPath()+"'");
            for (File ff : f.listFiles()) {
                if (!ff.delete()) {
                    throw new IOException("Could not delete old file '" +
                            ff.getPath() + "'");
                }
            }
            if (!f.delete()) {
                throw new IOException("Could not delete old file '" +
                        f.getPath() + "'");
            }
        // create the incoming dir
        }
        if (!f.mkdirs()) {
            throw (new IOException("Could not create incoming " +
                    "job direcotry '" + incomingDir + "'"));
        }
    } // check JobDir


    private String timeStamp() {
        Calendar g = new GregorianCalendar(); // defaults to now()

        int sec = g.get(Calendar.SECOND);
        int min = g.get(Calendar.MINUTE);
        int hour = g.get(Calendar.HOUR_OF_DAY);
        int mday = g.get(Calendar.DAY_OF_MONTH);
        int mon = g.get(Calendar.MONTH) + 1;  // JAN = 1

        int year = g.get(Calendar.YEAR);
        Formatter f = new Formatter();
        f.format("%04d%02d%02d-%02d%02d%02d", year, mon, mday, hour, min, sec);
        return f.toString();
    }

    public void openOutput() throws IOException {
        checkIncomingDir();
        currentFileName = "/" + namePrefix + "-" + timeStamp();
        outFileName = incomingDir + "/" + currentFileName;
        fos = new FileOutputStream(outFileName, true);
    }

    /** Closes and commits the output file 
     * Makes sure the committed directory exists
     * Moves the harvested file into the committed dir
     * Removes the direcotry from incoming
     * 
     * @throws java.io.IOException
     */
    public void closeOutput() throws IOException {
        fos.close();
        File fc = new File(committedDir);
        if (!fc.exists()) {
            if (!fc.mkdirs()) {
                throw new IOException("Could not create commit-dir '" +
                    fc.getPath() + "'");
            }
        }
        File fi = new File(incomingDir + "/" + currentFileName);
        if (!fi.renameTo(new File(committedDir + "/" + currentFileName))) {
            throw new IOException("Could not commit harvested file '" +
                    fi.getPath() + "'");
        }
        fi = new File(incomingDir);
        if (!fi.delete() ) {
            throw new IOException("Could not delete incoming dir'" +
                    fi.getPath() + "'");
        }
        logger.log(Level.FINE, "ZebraFileStorage: Committed '" +
                fc.getPath() + "'");
    } // closeOutput


    /** Remove all that we have on this job
     * 
     * @throws java.io.IOException
     */
    public void removeAll() throws IOException {
    }

    /** Close the output and remove the current file     
     * 
     * @throws java.io.IOException
     */
    public void closeAndDelete() throws IOException {
        fos.close();
        File f = new File(outFileName);
        if ( ! f.delete() ) {
            throw new IOException("Could not delete harvested file '" +
                    f.getPath() + "'");
        }
        f = new File(incomingDir);
        if (!f.delete() ) {
            throw new IOException("Could not delete incoming dir'" +
                    f.getPath() + "'");
        }
    } // closeAndDelete
    
    public OutputStream getOutputStream() {
        return fos;
    }

    public String getOutFileName() {
        return outFileName;
    }

} // ZebraFileStorage
