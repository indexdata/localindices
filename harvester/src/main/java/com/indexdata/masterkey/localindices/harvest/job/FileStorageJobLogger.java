package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;

public class FileStorageJobLogger extends StorageJobLogger {

  
  public FileStorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    super(loggerClass, resource);
  }

  public FileStorageJobLogger(Class<? extends Object> loggerClass, Storage storage) {
    super(loggerClass, storage);
  }

  protected void setupAppender(Class<? extends Object> loggerClass, String logFilename, String type) {
    try {
      if (getLogger().getAppender(logFilename) == null) {
	RollingFileAppender rolling = new RollingFileAppender(layout, logFilename, true);
	// TODO configurable 
	rolling.setMaxBackupIndex(10);
	rolling.setMaxFileSize("100KB");
	logAppender = rolling;	
	logAppender.setName(logFilename);
	logger.addAppender(logAppender);
      }
    } catch (IOException e) {
      logger = Logger.getLogger(loggerClass);
      logger.error("Failed to open per-" + type + " log file (" + logFilename + ")");
    }
  }

}
