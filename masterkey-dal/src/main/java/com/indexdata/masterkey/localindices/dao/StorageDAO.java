package com.indexdata.masterkey.localindices.dao;

import java.io.InputStream;
import java.util.List;

import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;

public interface StorageDAO {
    public void createStorage(Storage harvestable);
    public Storage retrieveStorageById(Long id);
    public Storage updateStorage(Storage hable);
    public void deleteStorage(Storage storage);
    public List<Storage> retrieveStorages(int start, int max);
    public InputStream getStorageLog(long id);
    /**
     * Retrieve a list of brief (listing) storages.
     * @return
     */
    List<StorageBrief> retrieveStorageBriefs(int start, int max);
    /**
     * Retrieves a Storage using it's listing reference (brief)
     * @param hbrief brief (listing) Storage
     * @return Storage detailed 
     */
    Storage retrieveFromBrief(StorageBrief hbrief);
    
    int getStorageCount();
	
	

}
