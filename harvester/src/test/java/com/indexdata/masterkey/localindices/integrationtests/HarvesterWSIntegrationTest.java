/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.integrationtests;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static com.indexdata.masterkey.localindices.integrationtests.IntTestUtil.*;
import static com.indexdata.utils.TextUtils.joinPath;
import org.w3c.dom.Document;

/**
 * A simple intergation test case to test some basics of the harvester WS
 * @author jakub
 */
public class HarvesterWSIntegrationTest {
  
  @Test
  public void testListJobs() throws TestException {
    Document res = GET(joinPath(ROOT_URI, "records/searchables/"));
    assertNotNull(res);
  }
}
