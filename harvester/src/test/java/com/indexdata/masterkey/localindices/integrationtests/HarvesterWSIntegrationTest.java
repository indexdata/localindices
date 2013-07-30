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
import static com.indexdata.utils.XmlUtils.serialize;
import org.w3c.dom.Document;

import static java.lang.System.out;
import javax.xml.transform.TransformerException;

/**
 * A simple intergation test case to test some basics of the harvester WS
 * @author jakub
 */
@SuppressWarnings("unused")
public class HarvesterWSIntegrationTest {
  
  @Test
  public void testListJobs() throws TestException, TransformerException {
    Document res = GET(joinPath(ROOT_URI, "records/searchables/"));
    serialize(res, out);
    assertNotNull(res);
  }
  
}
