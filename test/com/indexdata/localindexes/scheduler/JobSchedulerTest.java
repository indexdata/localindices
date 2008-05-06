/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.scheduler.dao.bean.HarvestableDAOFake;
import java.util.logging.Logger;
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
public class JobSchedulerTest {
    private static JobScheduler js;

    public JobSchedulerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {  
        js = new JobScheduler(new HarvestableDAOFake(),
                Logger.getLogger("com.indexdata.masterkey.test"));
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
     * Test of updateJobs method, of class JobScheduler.
     */
    @Test
    public void testUpdateJobs() {
        System.out.println("updateJobs");
        js.updateJobs();
    }

    /**
     * Test of checkJobs method, of class JobScheduler.
     */
    @Test
    public void testCheckJobs() {
        System.out.println("checkJobs");
        js.checkJobs();
    }

}