/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.util;

import com.indexdata.utils.ISOLikeDateParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Logger;

/**
 * 
 * @author jakub
 */
public class HarvestableLog {
  private final String logDir;
  private final long jobId;
  private final static int dateFieldLength = "YYYY-mm-dd HH:mm:ss,SSS".length();
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices");

  public HarvestableLog(String logDir, long jobId) {
    this.logDir = logDir;
    this.jobId = jobId;
  }

  public String readLines(Date from) throws FileNotFoundException, IOException {
    File[] candidates = getLogFiles(from);
    if (candidates != null && candidates.length > 0) {
      StringBuilder sb = new StringBuilder();
      for (File candidate : candidates) {
        if (candidate.exists() && candidate.isFile()) {
          logger.debug("Parsing logfile: "+candidate.getName());
          BufferedReader r = new BufferedReader(new FileReader(candidate));
          String line;
          boolean passthrough = from == null;
          while ((line = r.readLine()) != null) {
            //first column is the ISO date
            if (passthrough) {
              //all following entries must be older, just pass them through
              sb.append(line).append("\n");
            } else {
              //skip initial lines without timestamp
              if (line.length() >= dateFieldLength) {
                String dateStr = line.substring(0, dateFieldLength);
                try {
                  Date date = ISOLikeDateParser.parse(dateStr, TimeZone.getDefault());
                  if (date.after(from) || date.equals(from)) {
                    passthrough = true;
                    sb.append(line).append("\n");
                  }
                } catch  (ParseException pe) {
                  //continue
                }
              } //continue
            }
          }
          r.close();
        }
      }
      return sb.toString();
    } else {
      return null;
    }
  }

  private File[] getLogFiles(final Date from) throws FileNotFoundException {
    File dir = new File(logDir);
    if (!dir.isDirectory()) {
      throw new FileNotFoundException("Log directory not found: "+logDir);
    }
    final String logPrefix = "job-" + jobId + ".log";
    //candidate files must be updated after from date
    File[] logs = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith(logPrefix) &&
          (from == null || file.lastModified() >= from.getTime());
      }
    });
    if (logs != null) Arrays.sort(logs, Collections.reverseOrder());
    return logs;
  }

  public String getLogDir() {
    return logDir;
  }
  
  public long getJobId() {
    return jobId;
  }
  
}
