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
  public static HarvestStorage getStorage(Harvestable harvestable) {
    HarvestStorage harvestStorage = null;
    Storage entity = (Storage) harvestable.getStorage();
    /* TODO Extend to create other types */
    if (entity instanceof com.indexdata.masterkey.localindices.entity.SolrStorageEntity) {
      SolrStorage storage = new BulkSolrRecordStorage(entity.getUrl(), harvestable);
      storage.setStorageId(entity.getId().toString());
      return storage;
    }
    else if (entity instanceof FileStorageEntity) {
      FileStorage storage = new FileStorage();
      storage.setHarvestable(harvestable);
      return storage;
    }
    else {
      if (entity.getIdAsString() != null) {
	try {
	  Class<?> storage = Class.forName(entity.getIdAsString());
	  Object object = storage.newInstance();
	  if (object instanceof HarvestStorage) {
	    return (HarvestStorage) object;
	  }
	} catch (ClassNotFoundException e) {
	  e.printStackTrace();
	} catch (InstantiationException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
	} catch (IllegalAccessException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
	}	
      }
    }
    return harvestStorage;
  }

}
