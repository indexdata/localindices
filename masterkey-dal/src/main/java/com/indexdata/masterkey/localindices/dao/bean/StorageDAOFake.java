/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
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

    	Storage storage = new SolrStorageEntity();
		storage.setId(newStorageId());
		storage.setName("Local Index");
		storage.setUrl("http://localhost:8080/solr");
		storage.setEnabled(true);
		storages.put(storage.getId(), storage);
		
		Storage storage2 = new SolrStorageEntity();
		storage2.setId(newStorageId());
		storage2.setName("Staging Index");
		storage2.setUrl("http://test.indexdata.com/solr");
		storage2.setEnabled(true);
		storages.put(storage2.getId(), storage2);

		Storage storage3 = new SolrStorageEntity();
		storage3.setId(newStorageId());
		storage3.setName("Production Index");
		storage3.setUrl("http://zookeeper.indexdata.com/solr");
		storage3.setEnabled(false);
		storages.put(storage3.getId(), storage3);
    }

    public List<StorageBrief> retrieveBriefs(int start, int max) {
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
    
    synchronized private Long newStorageId() 
    {
    	long index = 1l;    	
    	for (Storage storage : storages.values()) {
			if (index <= storage.getId()) {
				index = storage.getId() + 1l;
    		}
    	}
		return index;
	}
    
    public Storage update(Storage storage) { 
        Storage hclone = null;
        try {
            hclone = (Storage) storage.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        if (hclone.getId() == null)
        	hclone.setId(newStorageId());
        storages.put(hclone.getId(), hclone);
        return hclone;
    }

    public void create(Storage storage) {
        if (storage.getId() == null)
        	storage.setId(newStorageId());
    	storages.put(storage.getId(), storage);    	
    }

    public Storage retrieveById(Long id) {
    	return storages.get(id);
    }

    public Storage updateStorage(Storage storage, Storage updStorage) {
    	return storages.put(storage.getId(), updStorage);
    }

    public void delete(Storage storage) {
    	storages.remove(storage.getId());
    }

    public List<Storage> retrieve(int start, int max) {
    	List<Storage> list = new LinkedList<Storage>();
    	for (Storage storage : storages.values()) 
    		list.add(storage);
    	return list;
    }

    public int getCount() {
        return storages.values().size();
    }

    @Override
    public InputStream getLog(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

  @Override
  public List<Storage> retrieve(int start, int max, String sortKey, boolean asc) {
    return retrieve(start, max);
  }

  @Override
  public List<StorageBrief> retrieveBriefs(int start, int max, String sortKey,
    boolean asc) {
    return retrieveBriefs(start, max, sortKey, asc);
  }
}
