/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StoragesConverter;
import com.indexdata.rest.client.ResourceConnectionException;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author jakub
 */
public class StorageDAOWS extends CommonDAOWS implements StorageDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public StorageDAOWS(String serviceBaseURL) {
        super(serviceBaseURL);
    }

    @Override
    public void create(Storage storage) {
        try {
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        StorageConverter storageContainer = new StorageConverter();
        storageContainer.setEntity(storage);
        URL url = storagesConnector.postAny(storageContainer);
    	storage.setId(extractId(url));

        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    /**
     * GET storage from the Web Service
     * @param id of entity to be fetched
     */
    @Override
    public Storage retrieveById(Long id) {
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

    /**
     * Retrieve list of all storages from the Web Service
     * @return
     */
    @Override
    public List<StorageBrief> retrieveBriefs(int start, int max, EntityQuery query) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max + query.asUrlParameters();
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
    public Storage update(Storage storage) {
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
    public void delete(Storage storage) throws EntityInUse {
        try {
            ResourceConnector<StorageConverter> storageConnector =
                new ResourceConnector<StorageConverter>(
                    new URL(serviceBaseURL + storage.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            storageConnector.delete();
        } catch (ResourceConnectionException rce) {
            if (EntityInUse.ERROR_MESSAGE.equals(rce.getServerMessage())) {
              throw new EntityInUse("Storage is in use", rce);
            }
        } catch (MalformedURLException male) {
          logger.warn(male.getMessage(), male);
        }
    }

    @Override
    public List<Storage> retrieve(int start, int max, EntityQuery query) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveBrief instead.");
       List<Storage> hables = new ArrayList<Storage>();
       List<StorageBrief> hrefs = retrieveBriefs(start, max, query);
       if (hrefs != null) {
            for (StorageBrief href : hrefs) {
                Storage hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;    
    }
    
    @Override
    public int getCount(EntityQuery query) {
        String url = serviceBaseURL + "?start=0&max=0" + query.asUrlParameters();
        try {
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            StoragesConverter hc = storagesConnector.get();
            return hc.getCount();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
            return 0;
        }
        
    }


    @Override
    public InputStream getLog(long id) {
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

  @Override
  public List<Storage> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
    return retrieve(start, max, query);
  }

  @Override
  public List<StorageBrief> retrieveBriefs(int start, int max, String sortKey,
    boolean asc, EntityQuery query) {
    return retrieveBriefs(start, max, sortKey, asc, query);
  }
}
