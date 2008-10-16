/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * This class handles bulk HTTP download of a single file.
 * @author jakub
 */
public class BullkHarvestJob implements HarvestJob {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private XmlBulkResource resource;
    private boolean die = false;
    
    public BullkHarvestJob(XmlBulkResource resource) {
        this.resource = resource;
        String persistedStatus = resource.getCurrentStatus();
        if (persistedStatus == null)
            this.status = HarvestStatus.NEW;
        else
            this.status = HarvestStatus.WAITING;
        this.resource.setError(null);
    }
    
    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.WARN, "Bulk harvest received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }
    
    public void kill() {
        if (status != HarvestStatus.FINISHED) {
            status = HarvestStatus.KILLED;
            onKillSendt();
        }
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public HarvestStorage getStorage() {
        return storage;
    }

    public void finishReceived() {
        status = HarvestStatus.WAITING;
    }

    public String getError() {
        return error;
    }

    public void run() {
        try {
            status = HarvestStatus.RUNNING;
            downloadList(resource.getUrl().split(" "));
            status = HarvestStatus.FINISHED;
        } catch (Exception e) {
            status = HarvestStatus.ERROR;
            error = e.getMessage();
            resource.setError(e.getMessage());
            logger.log(Level.ERROR,  "Download failed.", e);
        }
    }
    
    private void downloadList(String[] urls) throws Exception {
        for (String url : urls) {
            download(url);
        }
    }
    
    private void download(String urlString) throws Exception {
        logger.log(Level.INFO, "Starting download - " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            int contentLenght = conn.getContentLength();
            if (responseCode == 200) {
                try {
                    storage.begin();
                    pipe(conn.getInputStream(), storage.getOutputStream(), contentLenght);
                    storage.commit();
                } catch (IOException ioe) {         
                    storage.rollback();
                    throw new Exception("Storage write failed. ", ioe);
                }
            } else {
                throw new Exception("Http connection failed. (" + responseCode + ")");
            }
            logger.log(Level.INFO, "Finished - " + urlString);
        } catch (IOException ioe) {
            throw new Exception("Http connection failed.", ioe);
        }
    }
    
    private void pipe(InputStream is, OutputStream os, int total) throws IOException {
        int blockSize = 4096;
        int copied = 0;
        int num = 0;
        int logBlockNum = 256; //how many blocks to log progress
        byte[] buf = new byte[blockSize];
        for (int len = -1; (len = is.read(buf)) != -1;) {
            os.write(buf, 0, len);
            if (isKillSendt()) throw new IOException("Download interputed with a kill signal.");
            // every megabyte
            copied += len;
            if (num % logBlockNum == 0)
                logger.log(Level.INFO, "Downloaded " + copied + "/" + total + " bytes (" + ((double)copied/(double)total*100) +"%)");
            num++;
        }
        logger.log(Level.INFO, "Download finishes: " + copied + "/" + total + " bytes (" + ((double) copied/ (double) total*100) +"%)");
        os.flush();
    }

}
