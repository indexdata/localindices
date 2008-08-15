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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles bulk HTTP download of a single file.
 * @author jakub
 */
public class BullkHarvestJob implements HarvestJob {
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private XmlBulkResource resource;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey");
    
    public BullkHarvestJob(XmlBulkResource resource) {
        this.resource = resource;
        this.status = HarvestStatus.NEW;
    }
    
    public void kill() {
        try {
            storage.purge();
            status = HarvestStatus.KILLED;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "", ioe);
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
        } catch (Exception e) {
            status = HarvestStatus.ERROR;
            logger.log(Level.SEVERE, Thread.currentThread().getName() + "Download failed.", e);
        }
        status = HarvestStatus.FINISHED;
    }
    
    private void downloadList(String[] urls) throws Exception {
        for (String url : urls) {
            download(url);
        }
    }
    
    private void download(String urlString) throws Exception {
        logger.log(Level.INFO, Thread.currentThread().getName() + ": Starting download - " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try {
                    storage.begin();
                    pipe(conn.getInputStream(), storage.getOutputStream(), 4096);
                    storage.commit();
                } catch (IOException ioe) {         
                    storage.rollback();
                    throw new Exception("Storage write failed. ", ioe);
                }
            } else {
                throw new Exception("Http connection failed. (" + responseCode + ")");
            }
            logger.log(Level.INFO, Thread.currentThread().getName() + ": Download finished. " + urlString);
        } catch (IOException ioe) {
            throw new Exception("Http connection failed.", ioe);
        }
    }
    
    private void pipe(InputStream is, OutputStream os, int streamBuffSize) throws IOException {
        byte[] buf = new byte[streamBuffSize];
        for (int len = -1; (len = is.read(buf)) != -1;) {
            os.write(buf, 0, len);
        }
        os.flush();
    }

}
