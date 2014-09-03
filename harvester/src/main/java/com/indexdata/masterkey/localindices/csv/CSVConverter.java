/*
 * Copyright (c) 1995-2014, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.csv;

import com.indexdata.utils.XmlUtils;
import com.indexdata.xml.filter.MessageConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;

/**
 *
 * @author jakub
 */
public class CSVConverter {
  private String charset = "iso-8859-1";
  private char delimiter = ',';
  private boolean containsHeader = true;
  private CSVFormat format;

  public CSVConverter(String configuration) throws ParseException {
    ContentTypeParameters parser = new ContentTypeParameters();
    Map<String, String> params = parser.parse(configuration);
    if (params.containsKey("charset"))
      charset = params.get("charset");
    if (params.containsKey("delimiter"))
      delimiter = params.get("delimiter").charAt(0);
    if (params.containsKey("header"))
      containsHeader = "yes".equalsIgnoreCase(params.get("header"));
    format = CSVFormat.newFormat(delimiter).
      withIgnoreEmptyLines(true).
      withIgnoreSurroundingSpaces(true).
      withRecordSeparator("\r\n").
      withQuote('"');
    format = containsHeader ? format.withHeader() : format;
    
  }
  
  public void processViaDOM(InputStream is, MessageConsumer mc, boolean split) throws UnsupportedEncodingException, IOException {
    Reader reader = new InputStreamReader(is, charset);
    CSVParser parser = format.parse(reader);
    Document doc = split ? XmlUtils.newDoc("items") : null;
    for (CSVRecord r : parser) {
      doc = split ? doc : XmlUtils.newDoc("item");
      doc.createElement("field");
      mc.accept(doc);
    }
    
  }
  
}
