/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.util;

import com.sun.xml.bind.StringInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
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
public class TextUtilsTest {

    public TextUtilsTest() {
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
     * Test of copyStream method, of class TextUtils.
     */
    @Test
    public void testCopyStreamWithReplace() {
        String input = "this is some input text with a !replace! that should be substituted";
        String expected = "this is some input text with a wildcard that should be substituted";
        InputStream is = new ByteArrayInputStream(input.getBytes());
        OutputStream os = new ByteArrayOutputStream();
        try {
            TextUtils.copyStreamWithReplace(is, os, "!replace!", "wildcard");
        } catch (IOException ex) {
            fail();
        }
        String result = os.toString();
        System.out.println("Expected: " + expected);
        System.out.println("Result:" + result);
        assertEquals(expected, result);
    }

}