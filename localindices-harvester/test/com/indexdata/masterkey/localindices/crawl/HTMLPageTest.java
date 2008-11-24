/*
 *  Copyright (c) 1995-2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.crawl;

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
public class HTMLPageTest {
    private HTMLPage instance;

    public HTMLPageTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try { 
            instance = new HTMLPage(new URL("http://bagel.indexdata.com/cf"));
        } catch (MalformedURLException e) {
            fail("Cannot instantiate test class");
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getLinks method, of class HTMLPage.
     */
    @Test
    public void testGetLinks() {
        System.out.println("getLinks");
        for (URL link : instance.getLinks()) {
            System.out.println(link);
        }
    }

    /**
     * Test of xmlFragment method, of class HTMLPage.
     */
    @Test
    public void testToPazpar2Metadata() {
        System.out.println("xmlFragment");
        System.out.println(instance.toPazpar2Metadata());
    }

}