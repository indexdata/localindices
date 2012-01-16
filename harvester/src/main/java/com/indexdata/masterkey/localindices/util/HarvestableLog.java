/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author jakub
 */
public class HarvestableLog {
  // log file
  // TODO Should be configurable
  private static String logDir = "/var/log/masterkey/harvester/";

  // not used but shows the concept of matching against the whole buffer
  @SuppressWarnings("unused")
  private static void readLines(Pattern lp, CharBuffer in, StringBuilder out) {
    Matcher lm = lp.matcher(in); // Line matcher
    int lines = 0;
    while (lm.find()) {
      lines++;
      out.append(lm.group()); // The current line
      if (lm.end() == in.limit())
	break;
    }
  }

  public static String getHarvestableLog(long jobId) throws FileNotFoundException, IOException {
    
    BufferedReader r = new BufferedReader(new FileReader(getHarvesteableJobFilename(jobId)));
    StringBuilder sb = new StringBuilder(10240);
    String line;
    while ((line = r.readLine()) != null) {
      sb.append(line + "\n");
    }
    r.close();
    return sb.toString();
  }

  public static String getHarvesteableJobFilename(long jobId) {
    	return logDir + "job-" + jobId + ".log";
  }
}
