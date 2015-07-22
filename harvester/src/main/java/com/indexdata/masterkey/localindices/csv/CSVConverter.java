/*
 * Copyright (c) 1995-2014, Index Data
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author jakub
 */
public class CSVConverter {
  private final String charset;
  private final char delimiter;
  private final boolean containsHeader;
  private final String headerLine;
  private CSVFormat format;
  
  public CSVConverter(String configuration) throws ParseException {
    this("iso-8859-1", ',', configuration);
  }
  
  public CSVConverter(String defaultCharset, char defaultDelimiter, String configuration) throws ParseException {
    this(defaultCharset, defaultDelimiter, true, configuration);
  }

  protected CSVConverter(
    String defaultCharset, 
    char defaultDelimiter,
    boolean defaultContainsHeader,
    String configuration) throws ParseException {
    ContentTypeParameters parser = new ContentTypeParameters();
    Map<String, String> params = parser.parse(configuration);
    charset = params.containsKey("charset")
      ? params.get("charset")
      : defaultCharset;
    delimiter = params.containsKey("delimiter")
      ? params.get("delimiter").charAt(0)
      : defaultDelimiter;
    containsHeader = params.containsKey("containsHeader")
      ? "yes".equalsIgnoreCase(params.get("containsHeader"))
      : true;
    headerLine = params.containsKey("headerLine") 
      ? params.get("headerLine")
      : null;
    //default format does not handle headers
    format = CSVFormat.newFormat(delimiter).
      withIgnoreEmptyLines(true).
      withIgnoreSurroundingSpaces(true).
      withQuote('"');
    //we use specified format to parse the header line itself
    String[] headerNames = headerLine != null 
      ? parseHeaderNames(headerLine, CSVFormat.DEFAULT)
      : null;
    format = containsHeader 
      ? headerLine == null 
        ? format.withHeader()
        : format.withSkipHeaderRecord(true).withHeader(headerNames)
      : headerLine == null
        ? format
        : format.withHeader(headerNames);
  }
  
  public String getFormatString() {
    return "charset="+charset+"; delimiter="+delimiter
      +"; containsHeader=" + (containsHeader ? "yes" : "no")
      + (headerLine != null ? "; headerLine="+headerLine : "");
  }
  
  public void processViaDOM(InputStream is, MessageConsumer mc, boolean split) throws UnsupportedEncodingException, IOException {
    Reader reader = new InputStreamReader(is, charset);
    CSVParser parser = format.parse(reader);
    Document doc = split ? null : XmlUtils.newDoc("rows");
    Map<Integer, String> headers = null;
    if (containsHeader || headerLine != null) {
      //invert the weird header map
      Map<String, Integer> headerMap = parser.getHeaderMap();
      headers = new HashMap<Integer, String>(headerMap.size());
      for (Entry<String, Integer> entry : parser.getHeaderMap().entrySet()) {
        headers.put(entry.getValue(), entry.getKey());
      }
    }
    for (CSVRecord r : parser) {
      Element root;
      if (split) {
        //doc is null
        doc = XmlUtils.newDoc("row");
        root = doc.getDocumentElement();
      } else {
        root = doc.createElement("row");
        doc.getDocumentElement().appendChild(root);
      }
      for (int i=0; i<r.size(); i++) {
        Element fieldE = doc.createElement("field");
        if (containsHeader || headerLine != null) {
          String name = headers.get(i);
          //one indexed
          name = name != null ? name : String.valueOf(i+1);
          fieldE.setAttribute("name", name);
        } else {
          //one indexed
          fieldE.setAttribute("name", String.valueOf(i+1));
        }
        fieldE.setTextContent(r.get(i));
        root.appendChild(fieldE);
      }
      //sent chunk
      if (split)
        mc.accept(doc);
    }
    //sent whole doc
    if (!split) {
      mc.accept(doc);
    }
  }

  private String[] parseHeaderNames(String headerLine, CSVFormat format) throws ParseException {
    try {
      StringReader sr = new StringReader(headerLine);
      CSVParser parser = format.parse(sr);
      List<CSVRecord> lines = parser.getRecords();
      if (lines.size() > 0) {
        CSVRecord headerRecord = lines.get(0);
        String[] headers = new String[headerRecord.size()];
        for (int i=0; i<headerRecord.size(); i++) {
          headers[i] = headerRecord.get(i);
        }
        return headers;
      }
      return new String[0];
    } catch (IOException ioe) {
      throw new ParseException("Can't parse header configuration", 0);
    }
  }
  
}
