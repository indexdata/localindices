/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.cache;

import com.indexdata.utils.TextUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;


/**
 *
 * @author jakub
 */
public class DiskCache {
  private static String basePath;
  private final String jobPath;
  private final File jobPathDir;
  private final long jobId;

  public DiskCache(long jobId) {
    if (basePath == null)
      throw new IllegalStateException("cache basePath is null, make sure harvester is properly deployed");
    this.jobId = jobId;
    jobPath = TextUtils.joinPath(basePath, Long.toString(jobId));
    jobPathDir = new File(jobPath);
      
  }
  
  public void init() throws IOException {
    boolean created = jobPathDir.mkdir();
    if (!created) {
      //if already exists, we are OK
      if (!jobPathDir.isDirectory())
        throw new IOException("Cannot create cache dir at "+jobPathDir.getAbsolutePath());
    }    
  }
  
  public String getBasePath() {
    return basePath;
  }
  
  public String getJobPath() {
    return jobPath;
  }

  public File getJobPathDir() {
    return jobPathDir;
  }
  
  //package private on purpose
  static void setBasePath(String basePath) {
    DiskCache.basePath = basePath;
  }

  public long getJobId() {
    return jobId;
  }
  
  public String[] list() throws IOException {
    String[] list = jobPathDir.list();
    if (list == null) {
      throw new IOException("Cache directory does not exist at "+jobPathDir.getAbsolutePath());
    }
    Arrays.sort(list);
    for (int i=0; i<list.length; i++) {
      list[i] = String.format("%s/%s", jobPathDir.getAbsolutePath(), list[i]);
    }
    return list;
  }
  
  public void empty() throws IOException {
    File[] files = jobPathDir.listFiles();
    if (files == null) {
      throw new IOException("Cache directory does not exist at "+jobPathDir.getAbsolutePath());
    }
    for (File f : jobPathDir.listFiles()) {
      if (!f.delete()) throw new IOException("Cannot remove cache file at "+f.getAbsolutePath());
    }
  }
  
  public void purge() throws IOException {
    empty();
    jobPathDir.delete();
  }
  
  public String proposeName() {
    return String.format("%019d%s", new Date().getTime(), ".data");
  }
 
}
