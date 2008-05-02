/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.service.client;

import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.entity.OaiPmhResource;
import com.indexdata.localindexes.web.service.converter.HarvestableConverter;
import com.indexdata.localindexes.web.service.converter.HarvestablesConverter;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jakub
 */
public class ResourceConnectorTest {
    private static String baseURL = "http://localhost:8080/localindexes/resources/harvestables/";
    private static ResourceConnector<HarvestablesConverter> harvestablesConnector;
    private ResourceConnector<HarvestableConverter> harvestableConnector;
    private URL resourceURL;
    private Harvestable harvestable;

    public ResourceConnectorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        /* 
         * set up connector used throughout the test
         */
        try {
        System.out.println("+++ Creating harvestables connector.");
        harvestablesConnector =
                new ResourceConnector<HarvestablesConverter>(
                new URL(baseURL),
                "com.indexdata.localindexes.web.entity" +
                ":com.indexdata.localindexes.web.service.converter");
        } catch (MalformedURLException male) {
            fail("Come on, at leat the URL has to be valid.");
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        System.out.println("+++ Creating a new harvestable.");

        harvestable = new OaiPmhResource();
        harvestable.setName("test entry");
        harvestable.setTitle("automatically posted harvestable");
        harvestable.setDescription("relevant description");
        harvestable.setEnabled(false);
        harvestable.setCurrentStatus("no status");
        harvestable.setMaxDbSize(320);
        harvestable.setScheduleString("0:1:1");

        HarvestableConverter harvestableContainer = new HarvestableConverter();
        harvestableContainer.setEntity(harvestable);
        
        System.out.println("+++ Storing the harvestable in the WS.");
        try {
            resourceURL = harvestablesConnector.postAny(harvestableContainer);
            // merge (set the right id)
            String[] dirs = resourceURL.getPath().split("/");
            harvestable.setId(new Long(dirs[dirs.length - 1]));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        System.out.println("+++ Creating connector to the harvestable.");        
        harvestableConnector =
                new ResourceConnector<HarvestableConverter>(
                resourceURL,
                "com.indexdata.localindexes.web.entity" +
                ":com.indexdata.localindexes.web.service.converter");
    }

    @After
    public void tearDown() {
        System.out.println("+++ Deleting the harvestable.");        
        try {
            harvestableConnector.delete();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of get method, of class ResourceConnector.
     */
    @Test
    public void get() {
        try {
            System.out.println("+++ Retrieving the harvestable.");        
            Harvestable result = harvestableConnector.get().getEntity();
            System.out.println("+++ Comparing to the reference harvestable.");
            assertEquals(harvestable, result);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of put method, of class ResourceConnector.
     */
    @Test
    public void put() {
        try {
            System.out.println("+++ Updating the harvestable with new values.");        
            Harvestable hClone = (Harvestable) harvestable.clone();
            hClone.setName("updated name");
            hClone.setTitle("updated title");
            HarvestableConverter hc = new HarvestableConverter();
            hc.setEntity(hClone);
            harvestableConnector.put(hc);
        } catch (Exception e){
            fail(e.getMessage());
        }
    }

    /**
     * Test of delete method, of class ResourceConnector.
     */
    @Test
    public void delete() {
        try {
            System.out.println("+++ Removing the harvestable.");
            harvestableConnector.delete();            
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of post method, of class ResourceConnector.
     */
    @Test
    public void post() throws Exception {
        fail("The test case is a prototype.");
    }

    /**
     * Test of postAny method, of class ResourceConnector.
     */
    @Test
    public void postAny() throws Exception {
         fail("The test case is a prototype.");
    }

}