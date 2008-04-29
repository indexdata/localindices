/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.service.client;

import com.indexdata.localindexes.web.entity.*;
import com.indexdata.localindexes.web.service.converter.*;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author jakub
 */
public class TestClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        try {
            System.out.println("+++ Retrieving harvestables:");
            String baseURL = "http://localhost:8080/localindexes/resources/";
            
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                        new URL(baseURL + "harvestables/"), 
                        "com.indexdata.localindexes.web.entity" +
                        ":com.indexdata.localindexes.web.service.converter");

            HarvestablesConverter hc = harvestablesConnector.get();
            for (HarvestableRefConverter ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            
            System.out.println("+++ Creating new harvestable.");
            
            Harvestable harvestable = new OaiPmhResource();
            harvestable.setName("test entry");
            harvestable.setTitle("automatically posted harvestable");
            harvestable.setDescription("relevant description");
            harvestable.setEnabled(false);
            harvestable.setCurrentStatus("no status");
            harvestable.setMaxDbSize(320);
            harvestable.setScheduleString("0:1:1");
            
            HarvestableConverter harvestableContainer = new HarvestableConverter();
            harvestableContainer.setEntity(harvestable);
            URL resourceURL = harvestablesConnector.postAny(harvestableContainer);
            
            System.out.println("+++ Identifier assigned to harvestable:");
            System.out.println(resourceURL);
            
            System.out.println("+++ Retrieving the created harvestable.");
            
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                        resourceURL, 
                        "com.indexdata.localindexes.web.entity" +
                        ":com.indexdata.localindexes.web.service.converter");
                        
            harvestable = harvestableConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved harvestable:");
            System.out.println("Harvestable id: " + harvestable.getId());
            System.out.println("Harvestable name: " + harvestable.getName());
            System.out.println("Harvestable title: " + harvestable.getTitle());
            System.out.println("Harvestable description: " + harvestable.getDescription());
            
            System.out.println("+++ Updating the harvestable with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Harvestable name: " + newName);
            System.out.println("Harvestable title: " + newTitle);
            
            Harvestable harvestableCopy = (Harvestable) harvestable.clone();
            harvestable.setName(newName);
            harvestable.setTitle(newTitle);
            
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
}
