/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.SolrStorage;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;

/**
 *
 * @author jakub
 */
public class StorageDAOFake implements StorageDAO {
    private Map<Long, Storage> storages;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    public StorageDAOFake() {
    	storages = new HashMap<Long, Storage>();

    	SolrStorage storage = new SolrStorage();
		storage.setId(new Long(1));
		storage.setName("Test Local Index");
		storage.setUrl("http://localhost:8983");
		
		storages.put(storage.getId(), storage);
		
		Storage storage2 = new SolrStorage();
		storage2.setId(new Long(2));
		storage2.setName("University of Groningen");
//            storage2.setUrl("http://ir.ub.rug.nl/oai/");
		
		storages.put(storage2.getId(), storage2);
    }

    public List<StorageBrief> retrieveStorageBriefs(int start, int max) {
        List<StorageBrief> srefs = new ArrayList<StorageBrief>();
        for (Storage storage : storages.values()) {
            StorageBrief sref = new StorageBrief(storage);
			//sref.setResourceUri(new URI("http://localhost/harvestables/" + sref.getId() + "/)"));
			srefs.add(sref);
        }
        return srefs;
    }

    public Storage retrieveFromBrief(StorageBrief href) {
        try {
            return (Storage) storages.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);
        }
        return null;
    }

    public Storage updateStorage(Storage storage) { 
        Storage hclone = null;
        try {
            hclone = (Storage) storage.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        storages.put(hclone.getId(), hclone);
        return hclone;
    }

    public void createStorage(Storage storage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Storage retrieveStorageById(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Storage updateStorage(Storage storage, Storage updStorage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteStorage(Storage storage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Storage> retrieveStorages(int start, int max) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getStorageCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getStorageLog(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
