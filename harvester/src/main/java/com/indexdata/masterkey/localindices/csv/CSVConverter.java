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
import java.util.HashMap;
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
    Document doc = split ? null : XmlUtils.newDoc("rows");
    Map<Integer, String> headers = null;
    if (containsHeader) {
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
        if (containsHeader) {
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
  
}
