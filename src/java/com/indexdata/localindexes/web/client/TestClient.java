/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.client;

import com.indexdata.localindexes.web.entitybeans.*;
import com.indexdata.localindexes.web.converter.*;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

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
            String uri = "http://localhost:8080/localindexes/resources/harvestables/";
            ResourceConnector<HarvestablesConverter> rc =
                    new ResourceConnector<HarvestablesConverter>(
                        new URI(uri), HarvestablesConverter.class);

            HarvestablesConverter hc = rc.get();
            System.out.println(hc.getResourceUri());
            
            uri = "http://localhost:8080/localindexes/resources/harvestables/52/";
            ResourceConnector<HarvestableConverter> rc2 =
                    new ResourceConnector<HarvestableConverter>(
                        new URI(uri), "com.indexdata.localindexes.web.entitybeans" +
                        ":com.indexdata.localindexes.web.converter");
            Harvestable harvestable = rc2.get().getEntity();
                        
            System.out.println("+++ Retrieved harvestable resource:");
            System.out.println("Harvestable id: " + harvestable.getId());
            System.out.println("Harvestable title: " + harvestable.getTitle());
            System.out.println("Harvestable description: " + harvestable.getDescription());
            
            System.out.println("+++ creting a resource resource:");
            
            harvestable.setName("new posted resource");
            harvestable.setTitle("some title");
            
            HarvestableConverter postConverter = new HarvestableConverter();
            postConverter.setEntity(harvestable);
            
            rc2.post(postConverter);

        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
