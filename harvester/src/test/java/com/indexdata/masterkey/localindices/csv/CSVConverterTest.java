/*
 * Copyright (c) 1995-2014, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.csv;

import com.indexdata.utils.XmlUtils;
import com.indexdata.xml.filter.MessageConsumer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Node;

/**
 *
 * @author jakub
 */
public class CSVConverterTest {
  
  private static String csvWithHeaders = 
    "Title,Name,URL,Description\n" +
    "FooTitle, Foo Name, \"http://foo.com\", \"Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as delimiters\"\n" +
    "BarTitle, Bar Name, \"http://bar.com\", \"Another Description\"";
  
  private static String csvWithHeaders2 = 
    "Label,Name,URL,Description\n" +
    "FooTitle, Foo Name, \"http://foo.com\", \"Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as delimiters\"\n" +
    "BarTitle, Bar Name, \"http://bar.com\", \"Another Description\"";
  
  private static String xmlCsvWithHeadersNoSplit =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rows><row>"
    + "<field name=\"Title\">FooTitle</field>"
    + "<field name=\"Name\">Foo Name</field>"
    + "<field name=\"URL\">http://foo.com</field>"
    + "<field name=\"Description\">Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as"
    + " delimiters</field></row><row>"
    + "<field name=\"Title\">BarTitle</field>"
    + "<field name=\"Name\">Bar Name</field>"
    + "<field name=\"URL\">http://bar.com</field>"
    + "<field name=\"Description\">Another Description</field></row></rows>";
  
  private static String xmlCsvWithHeadersSplit1 =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><row>"
    + "<field name=\"Title\">FooTitle</field>"
    + "<field name=\"Name\">Foo Name</field>"
    + "<field name=\"URL\">http://foo.com</field>"
    + "<field name=\"Description\">Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as"
    + " delimiters</field></row>";
  
  private static String xmlCsvWithHeadersSplit2 =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><row>"
    + "<field name=\"Title\">BarTitle</field>"
    + "<field name=\"Name\">Bar Name</field>"
    + "<field name=\"URL\">http://bar.com</field>"
    + "<field name=\"Description\">Another Description</field></row>";

  private static String csvWithoutHeaders = 
    "FooTitle, Foo Name, \"http://foo.com\", \"Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as delimiters\"\n" +
    "BarTitle, Bar Name, \"http://bar.com\", \"Another Description\"";
  
  private static String xmlCsvWithoutHeadersSplit1 =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><row>"
    + "<field name=\"1\">FooTitle</field>"
    + "<field name=\"2\">Foo Name</field>"
    + "<field name=\"3\">http://foo.com</field>"
    + "<field name=\"4\">Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as"
    + " delimiters</field></row>";
  
  private static String xmlCsvWithoutHeadersSplit2 =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><row>"
    + "<field name=\"1\">BarTitle</field>"
    + "<field name=\"2\">Bar Name</field>"
    + "<field name=\"3\">http://bar.com</field>"
    + "<field name=\"4\">Another Description</field></row>";
  
  private static String xmlCsvWithoutHeadersNoSplit =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rows><row>"
    + "<field name=\"1\">FooTitle</field>"
    + "<field name=\"2\">Foo Name</field>"
    + "<field name=\"3\">http://foo.com</field>"
    + "<field name=\"4\">Some description, with coma's,"
    + " but because it is in quote, the comma's are not interpreted as"
    + " delimiters</field></row><row>"
    + "<field name=\"1\">BarTitle</field>"
    + "<field name=\"2\">Bar Name</field>"
    + "<field name=\"3\">http://bar.com</field>"
    + "<field name=\"4\">Another Description</field></row></rows>";
  
  static class TestConsumer implements MessageConsumer {
    final String[] expected;
    private int counter;
    
    public TestConsumer(String... expected) {
      this.expected = expected;
    }
    
    @Override
    public void accept(Node node) {
      if (counter >= expected.length) fail("More values generated than expected: "+counter);
      try {
        StringWriter sw = new StringWriter();
        XmlUtils.serialize(node, sw);
        assertEquals(expected[counter], sw.toString());
        counter++;
      } catch (TransformerException ex) {
        fail("cannot seriazlize");
      }
    }

    @Override
    public void accept(Source source) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  
  }
  
  @Test
  public void testProcessViaDOMWithHeadersSplit() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithHeadersSplit1, xmlCsvWithHeadersSplit2);
    boolean split = true;
    CSVConverter instance = new CSVConverter(""); //defaults
    InputStream is = new ByteArrayInputStream(csvWithHeaders.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
  @Test
  public void testProcessViaDOMWithoutHeadersSplit() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithoutHeadersSplit1, xmlCsvWithoutHeadersSplit2);
    boolean split = true;
    CSVConverter instance = new CSVConverter("containsHeader=no");
    InputStream is = new ByteArrayInputStream(csvWithoutHeaders.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
  @Test
  public void testProcessViaDOMWithHeadersNoSplit() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithHeadersNoSplit);
    boolean split = false;
    CSVConverter instance = new CSVConverter(""); //defaults
    InputStream is = new ByteArrayInputStream(csvWithHeaders.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
  @Test
  public void testProcessViaDOMWithoutHeadersNoSplit() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithoutHeadersNoSplit);
    boolean split = false;
    CSVConverter instance = new CSVConverter("containsHeader=no");
    InputStream is = new ByteArrayInputStream(csvWithoutHeaders.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
  @Test
  public void testProcessViaDOMOverrideHeader() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithHeadersNoSplit);
    boolean split = false;
    CSVConverter instance = new CSVConverter("headerLine=Title,Name,URL,Description");
    InputStream is = new ByteArrayInputStream(csvWithHeaders2.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
  @Test
  public void testProcessViaDOMSpecifyHeader() throws Exception {
    MessageConsumer mc = new TestConsumer(xmlCsvWithHeadersNoSplit);
    boolean split = false;
    CSVConverter instance = new CSVConverter("containsHeader=no; headerLine=Title,Name,URL,Description");
    InputStream is = new ByteArrayInputStream(csvWithoutHeaders.getBytes("iso-8859-1"));
    instance.processViaDOM(is, mc, split);
  }
  
}
