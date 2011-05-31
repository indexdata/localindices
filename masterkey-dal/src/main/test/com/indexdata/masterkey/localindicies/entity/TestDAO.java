package com.indexdata.masterkey.localindicies.entity;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.SolrStorage;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StoragesConverter;
import com.indexdata.rest.client.ResourceConnector;

import junit.framework.TestCase;

public class TestDAO extends TestCase {

    /**
     * @param args the command line arguments
     */
    public void testHarvestables() throws IOException {
        try {
            System.out.println("+++ Retrieving harvestables:");
            String baseURL = "http://localhost:8080/localindices-harvester/resources/";
            
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                        new URL(baseURL + "harvestables/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            HarvestablesConverter hc = harvestablesConnector.get();
            for (HarvestableBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            
            System.out.println("+++ Creating new harvestable.");
            
            Harvestable harvestable = new OaiPmhResource();
            harvestable.setName("test entry");
            harvestable.setServiceProvider("automatically posted harvestable");
            harvestable.setTechnicalNotes("relevant description");
            harvestable.setEnabled(false);
            harvestable.setCurrentStatus("no status");
            harvestable.setMaxDbSize(320);
            harvestable.setScheduleString("0:1:1");
            harvestable.setLastUpdated(new Date());
            
            HarvestableConverter harvestableContainer = new HarvestableConverter();
            harvestableContainer.setEntity(harvestable);
            URL resourceURL = harvestablesConnector.postAny(harvestableContainer);
            
            System.out.println("+++ Identifier assigned to harvestable:");
            System.out.println(resourceURL);
            
            System.out.println("+++ Retrieving the created harvestable.");
            
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            harvestable = harvestableConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved harvestable:");
            System.out.println("Harvestable id: " + harvestable.getId());
            System.out.println("Harvestable name: " + harvestable.getName());
            System.out.println("Harvestable title: " + harvestable.getServiceProvider());
            System.out.println("Harvestable description: " + harvestable.getTechnicalNotes());
            
            System.out.println("+++ Updating the harvestable with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Harvestable name: " + newName);
            System.out.println("Harvestable title: " + newTitle);
            
            Harvestable harvestableCopy = (Harvestable) harvestable.clone();
            harvestable.setName(newName);
            harvestable.setServiceProvider(newTitle);
            
            harvestableContainer.setEntity(harvestable);
            harvestableConnector.put(harvestableContainer);
            
            System.out.println("+++ Reverting the harvestable to original values.");
            harvestableContainer.setEntity(harvestableCopy);
            harvestableConnector.put(harvestableContainer);
            
            System.out.println("+++ Deleting created harvestable.");
            harvestableConnector.delete();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    
    public void testStorages() throws IOException {
        try {
            System.out.println("+++ Retrieving storages:");
            String baseURL = "http://localhost:8080/localindices-harvester/resources/";
            
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                        new URL(baseURL + "storages/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            StoragesConverter hc = storagesConnector.get();
            for (StorageBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            
            System.out.println("+++ Creating new Storage.");
            
            Storage storage = new SolrStorage();
            storage.setName("test entry");
            storage.setEnabled(false);
/*            storage.setCurrentStatus("no status");  */
            
            StorageConverter storageContainer = new StorageConverter();
            storageContainer.setEntity(storage);
            URL resourceURL = storagesConnector.postAny(storageContainer);
            
            System.out.println("+++ Identifier assigned to storage:");
            System.out.println(resourceURL);
            
            System.out.println("+++ Retrieving the created storage.");
            
            ResourceConnector<StorageConverter> storageConnector =
                    new ResourceConnector<StorageConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            storage = storageConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved storage:");
            System.out.println("Storage id: " + storage.getId());
            System.out.println("Storage name: " + storage.getName());
            
            System.out.println("+++ Updating the storage with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Storage name: " + newName);
            System.out.println("Storage title: " + newTitle);
            
            Storage storageCopy = (Storage) storage.clone();
            storage.setName(newName);
            
            storageContainer.setEntity(storage);
            storageConnector.put(storageContainer);
            
            System.out.println("+++ Reverting the storage to original values.");
            storageContainer.setEntity(storageCopy);
            storageConnector.put(storageContainer);
            
            System.out.println("+++ Deleting created storage.");
            storageConnector.delete();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

	
}
