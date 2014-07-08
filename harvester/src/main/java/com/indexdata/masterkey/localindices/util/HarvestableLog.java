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
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * 
 * @author jakub
 */
public class HarvestableLog {
  private final String logDir;
  private final long jobId;
  private final String emptyMsg;
  private final static int dateFieldLength = "YYYY-mm-dd HH:mm:ss,SSS".length();
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");

  public HarvestableLog(String logDir, long jobId) {
    this.logDir = logDir;
    this.jobId = jobId;
    this.emptyMsg = "--- WARNING: log file(s) for job " + jobId + " not found ---";
  }

  public String readLines(Date from) throws FileNotFoundException, IOException {
    File[] candidates = getLogFiles(from);
    if (candidates != null && candidates.length > 0) {
      StringBuilder sb = new StringBuilder();
      for (File candidate : candidates) {
        if (candidate.exists() && candidate.isFile()) {
          BufferedReader r = new BufferedReader(new FileReader(candidate));
          String line;
          while ((line = r.readLine()) != null) {
            //first column is the ISO date
            String dateStr = line.substring(0, dateFieldLength);
            try {
              Date date = ISOLikeDateParser.parse(dateStr);
              if (date.after(from))
                sb.append(line).append("\n");
            } catch  (ParseException pe) {
              logger.warn("Failed to parse date out of the following log line:" + line);
            }
          }
          r.close();
        }
      }
      return sb.toString();
    } else {
      return emptyMsg;
    }
  }

  private File[] getLogFiles(final Date from) {
    File dir = new File(logDir);
    if (!dir.isDirectory()) {
      return null;
    }
    final String logPrefix = "job-" + jobId + ".log";
    //candidate files must be updated after from date
    File[] logs = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith(logPrefix) &&
          file.lastModified() >= from.getTime();
      }
    });
    if (logs != null) Arrays.sort(logs);
    return logs;
  }

  public String getLogDir() {
    return logDir;
  }
  
  public long getJobId() {
    return jobId;
  }
  
}
