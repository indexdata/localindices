/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */
package com.indexdata.masterkey.localindices.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;

/**
 * 
 * @author jakub
 */
public class TextUtils {

  public static void copyStream(InputStream is, OutputStream os) throws IOException {
    byte[] buf = new byte[4096];
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
    }
    os.flush();
  }

  public static void copyStreamWithReplace(InputStream is, OutputStream os, String[] tokens)
      throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    int tokenLen = 0;
    if (tokens != null) {
      tokenLen = tokens.length % 2 == 1 ? tokens.length - 1 : tokens.length;
    }
    for (String line; (line = br.readLine()) != null;) {
      String replaced = line;
      for (int i = 0; i < tokenLen; i += 2) {
	replaced = replaced.replaceAll(tokens[i], tokens[i + 1]);
      }
      sb.append(replaced).append("\n");
    }
    br.close();
    os.write(sb.toString().getBytes());
  }

  public static String readStream(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append("\n");
    }

    br.close();
    return sb.toString();
  }
  
  public static String basename(String path) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length()-1);
    }
    int idx = path.lastIndexOf('/');
    return idx == -1 ? path : path.substring(idx+1);
  }

  public static String nodeToXMLString(Node node) {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      StringWriter stringWriter = new StringWriter();
      StreamResult result = new StreamResult(stringWriter);
      DOMSource domSource = new DOMSource(node);
      transformer.transform(domSource, result);
      return stringWriter.toString();
    } catch(Exception e) {
      return "Failed to transform node: " + e.getLocalizedMessage();
    }
  }
  
  
}
