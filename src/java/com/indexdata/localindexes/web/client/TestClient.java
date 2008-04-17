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
//                        "com.indexdata.localindexes.web.entitybeans" +
//                        ":com.indexdata.localindexes.web.converter");

            HarvestablesConverter hc = rc.get();
            System.out.println(hc.getResourceUri());

        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
