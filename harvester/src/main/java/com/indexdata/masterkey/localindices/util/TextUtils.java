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
}
