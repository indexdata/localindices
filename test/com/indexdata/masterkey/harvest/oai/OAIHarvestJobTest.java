/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.masterkey.harvest.oai;

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
public class OAIHarvestJobTest {
    private String baseURL = "http://arXiv.org/oai2";
    private String from = "2008-03-01";
    private String until = "2008-04-01";
    private String metadataPrefix = "oai_dc";
    private String setSpec;

    public OAIHarvestJobTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of run method, of class OAIHarvestJob.
     */
    @Test
    public void testRun() {
            //HarvestStorage storage = new FileStorage("data.harvest");
            HarvestStorage storage = new ConsoleStorage();

            HarvestJob oaijob 
                = new OAIHarvestJob(baseURL,
                                    from, until,
                                    metadataPrefix, setSpec); 

            oaijob.setStorage(storage);
            oaijob.run();
    }

    /**
     * Test of setStorage method, of class OAIHarvestJob.
     */
    @Test
    public void testSetStorage() {
        System.out.println("setStorage");
        HarvestStorage storage = null;
        OAIHarvestJob instance = null;
        instance.setStorage(storage);
    }

}