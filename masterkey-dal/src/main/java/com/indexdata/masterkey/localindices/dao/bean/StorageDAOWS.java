/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StoragesConverter;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author jakub
 */
public class StorageDAOWS implements StorageDAO {

    private String serviceBaseURL;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public StorageDAOWS(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    /**
     * Retrieve list of all storages from the Web Service
     * @return
     */
    @Override
    public List<StorageBrief> retrieveStorageBriefs(int start, int max) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max;
        try {
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            StoragesConverter hc = storagesConnector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
    }


    /**
     * Retrieve storage from the Web Service using it's reference (URL)
     * @param href storageRef entity
     * @return Storage entity
     */
    @Override
    public Storage retrieveFromBrief(StorageBrief href) {
        try {
            ResourceConnector<StorageConverter> storageConnector =
                    new ResourceConnector<StorageConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            return storageConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
    } // retrieveFromBrief

    /**
     * PUT storage to the Web Service
     * @param storage entity to be put
     */
    @Override
    public Storage updateStorage(Storage storage) {
        try {
            ResourceConnector<StorageConverter> storageConnector =
                    new ResourceConnector<StorageConverter>(
                    new URL(serviceBaseURL + storage.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            StorageConverter hc = new StorageConverter();
            hc.setEntity(storage);
            storageConnector.put(hc);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return storage;
    } // updateJob

    @Override
    public void createStorage(Storage storage) {
        try {
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        StorageConverter storageContainer = new StorageConverter();
        storageContainer.setEntity(storage);
        storagesConnector.postAny(storageContainer);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public Storage retrieveStorageById(Long id) {
        Storage hable = null;
        try {
            ResourceConnector<StorageConverter> storageConnector =
                new ResourceConnector<StorageConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            hable = storageConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG,  male);
        }
        return hable;    
    }

    @Override
    public void deleteStorage(Storage storage) {
        try {
            ResourceConnector<StorageConverter> storageConnector =
                new ResourceConnector<StorageConverter>(
                    new URL(serviceBaseURL + storage.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            storageConnector.delete();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public List<Storage> retrieveStorages(int start, int max) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveStorageBrief instead.");
       List<Storage> hables = new ArrayList<Storage>();
       List<StorageBrief> hrefs = retrieveStorageBriefs(start, max);
       if (hrefs != null) {
            for (StorageBrief href : hrefs) {
                Storage hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;    
    }
    
    @Override
    public int getStorageCount() {
        String url = serviceBaseURL + "?start=0&max=0";
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getCount();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
            return 0;
        }
        
    }


    @Override
    public InputStream getStorageLog(long id) {
        /* TODO Fix */
    	String logURL = serviceBaseURL + id + "/" + "log/";
        try {
            URL url = new URL(logURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return conn.getInputStream();
        } catch (IOException ioe) {
            logger.log(Level.DEBUG, ioe);
            return null;
        }
    }
}
