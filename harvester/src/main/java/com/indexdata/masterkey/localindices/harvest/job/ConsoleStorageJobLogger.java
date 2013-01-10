package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;

public class ConsoleStorageJobLogger extends StorageJobLogger {

  public ConsoleStorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    super(loggerClass, resource);
    logger.setLevel(Level.DEBUG);
  }

  public ConsoleStorageJobLogger(Class<? extends Object> loggerClass, Storage resource) {
    super(loggerClass, resource);
    logger.setLevel(Level.DEBUG);
  }

  protected void setupAppender(Class<? extends Object> loggerClass, String logFilename, String type) {
    if (logger.getAppender(logFilename) == null) {
      logAppender = new ConsoleAppender(layout);
      logAppender.setName(logFilename);
      logger.addAppender(logAppender);
    }
  }
}
