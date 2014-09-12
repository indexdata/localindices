/*
 * Copyright (c) 1995-2014, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.csv;

import java.text.ParseException;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author jakub
 */
public class ContentTypeParametersTest {
  
  public ContentTypeParametersTest() {
  }

  @Test
  public void testParseGood() throws ParseException {
    String goodParams = "one=a; two=b ; three=\"a=b\"; four=\";\"";
    ContentTypeParameters parser = new ContentTypeParameters();
    Map<String, String> params = parser.parse(goodParams);
    assertEquals("Parameter length", 4, params.size());
    assertEquals("a", params.get("one"));
    assertEquals("b", params.get("two"));
    assertEquals("a=b", params.get("three"));
    assertEquals(";", params.get("four"));
  }
  
  @Test
  public void testParseGood2() throws ParseException {
    String goodParams = "one  =a; two=  b ; three=\"a  =  b\"; four=\";\" ; ;;;";
    ContentTypeParameters parser = new ContentTypeParameters();
    Map<String, String> params = parser.parse(goodParams);
    assertEquals("Parameter length", 4, params.size());
    assertEquals("a", params.get("one"));
    assertEquals("b", params.get("two"));
    assertEquals("a  =  b", params.get("three"));
    assertEquals(";", params.get("four"));
  }
  
  @Test
  public void testParseEmpty() throws ParseException {
    String goodParams = "";
    ContentTypeParameters parser = new ContentTypeParameters();
    Map<String, String> params = parser.parse(goodParams);
    assertEquals("Parameter length", 0, params.size());
  }
  
  @Test
  public void testParseBad1() throws ParseException {
    String badParams = "one=; two=b ; three=\"a=b\"; four=\";\"";
    ContentTypeParameters parser = new ContentTypeParameters();
    boolean caught = false;
    try {
      Map<String, String> params = parser.parse(badParams);
    } catch (ParseException e) {
      caught = true;
    }
    assertTrue("Failed", caught);
  }
  
  @Test
  public void testParseBad3() throws ParseException {
    String badParams = "one; two=b ; three=\"a=b\"; four=\";\"";
    ContentTypeParameters parser = new ContentTypeParameters();
    boolean caught = false;
    try {
      Map<String, String> params = parser.parse(badParams);
    } catch (ParseException e) {
      caught = true;
    }
    assertTrue("Failed", caught);
  }
  
  @Test
  public void testParseBad4() throws ParseException {
    String badParams = "one two=b ; three=\"a=b\"; four=\";\"";
    ContentTypeParameters parser = new ContentTypeParameters();
    boolean caught = false;
    try {
      Map<String, String> params = parser.parse(badParams);
    } catch (ParseException e) {
      caught = true;
    }
    assertTrue("Failed", caught);
  }
  
  @Test
  public void testParseBad5() throws ParseException {
    String badParams = "one=\" ; two=b ; three=\"a=b\"; four=\";\"";
    ContentTypeParameters parser = new ContentTypeParameters();
    boolean caught = false;
    try {
      Map<String, String> params = parser.parse(badParams);
    } catch (ParseException e) {
      caught = true;
    }
    assertTrue("Failed", caught);
  }
  
  @Test
  public void testParseBad6() throws ParseException {
    String badParams = "one=\" ; two=b ; three=";
    ContentTypeParameters parser = new ContentTypeParameters();
    boolean caught = false;
    try {
      Map<String, String> params = parser.parse(badParams);
    } catch (ParseException e) {
      caught = true;
      throw e;
    }
    assertTrue("Failed", caught);
  }
  
}
