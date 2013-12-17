/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.FileStorageEntity;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;

/**
 * Returns an instance of a HarvestStorage object.
 * 
 * @author jakub
 * @author Dennis
 */
public class HarvestStorageFactory {

  /**
   * Creates a HarvestStorage based on the Database Entity
   * 
   * @param harvestable
   * @return
   */
  public static RecordStorage getStorage(Harvestable harvestable) {
    RecordStorage harvestStorage = null;
    Storage entity = (Storage) harvestable.getStorage();
    /* TODO Extend to create other types */
    if (entity.getCustomClass() != null && !"".equals(entity.getCustomClass())) {
	try {
	  Class<?> storage = Class.forName(entity.getCustomClass());
	  Object object = storage.newInstance();
	  RecordStorage recordStorage = (RecordStorage) object;
	  recordStorage.setHarvestable(harvestable);
	  return recordStorage;
	} catch (ClassNotFoundException e) {
	  e.printStackTrace();
	  throw new RuntimeException("Class not found: " +  entity.getCustomClass());
	} catch (InstantiationException e) {
	  e.printStackTrace();
	  throw new RuntimeException("Failed to create instance: " +  entity.getCustomClass());
	} catch (IllegalAccessException e) {
	  e.printStackTrace();
	  throw new RuntimeException("Illegal access: " +  entity.getCustomClass());
	}
    }
    else if (entity instanceof com.indexdata.masterkey.localindices.entity.ZkSolrStorageEntity) {
      SolrRecordStorage storage = new ZooKeeperSolrRecordStorage();
      storage.setHarvestable(harvestable);
      return storage;
    }
    else if (entity instanceof com.indexdata.masterkey.localindices.entity.SolrStorageEntity) {
      SolrRecordStorage storage = new BulkSolrRecordStorage();
      storage.setHarvestable(harvestable);
      return storage;
    }
    else if (entity instanceof FileStorageEntity) {
      FileStorage storage = new FileStorage();
      storage.setHarvestable(harvestable);
      return storage;
    }
    else {
      }
    return harvestStorage;
  }

}
