/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.scheduler;

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
public class SchedulerThreadTest {
    private static String serviceBaseURL = "http://localhost:8080/localindexes/resources/harvestables/";
    private static SchedulerThread st;

    public SchedulerThreadTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        st = new SchedulerThread(serviceBaseURL);
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
     * Test of run method, of class SchedulerThread.
     */
    @Test
    public void testRun() {
        st.run();
    }

    /**
     * Test of kill method, of class SchedulerThread.
     */
    @Test
    public void testKill() {
        st.kill();
    }

}