/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.harvest.oai;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.ConsoleStorage;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.job.HarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.OAIHarvestJob;
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
            OaiPmhResource resource = new OaiPmhResource();
            resource.setUrl(baseURL);
            resource.setMetadataPrefix(metadataPrefix);
            resource.setOaiSetName(setSpec);
            
            HarvestJob oaijob 
                = new OAIHarvestJob(resource); 

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