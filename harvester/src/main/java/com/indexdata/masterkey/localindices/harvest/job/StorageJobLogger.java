package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.util.HarvestableLog;


public class StorageJobLogger implements LocalIndicesLogger {

  private Logger logger; 
  private String identify;
  Layout layout = new PatternLayout("%d %-5p %m\n");
  Appender storageFileLog  = null;   
  Appender jobFileLog  = null;   
  public StorageJobLogger(Class<? extends Object> loggerClass, Storage resource) {
    
    String logId = "storage-" + (resource != null ? resource.getId() : "null");
    logger = Logger.getLogger(logId);
    String logFilename = "/var/log/masterkey/harvester/" + logId  + ".log";
    try {
      storageFileLog = new FileAppender(layout, logFilename, true);
      logger.addAppender(storageFileLog);
    } catch (IOException e) {
      logger = Logger.getLogger(loggerClass);
      logger.error("Failed to open per-job log file (" + logFilename + ")");
    }
    logger.setAdditivity(true);
    if (resource != null)
      setIdentify("STORAGE(" + resource.getId() + " " + resource.getName() + "): ");
  }

  public StorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    String logFilename = HarvestableLog.getHarvesteableJobFilename(resource.getId());
    logger = Logger.getLogger(loggerClass + "JOB#" + resource.getId() );
    try {
      // restart the log on each run
      jobFileLog = new FileAppender(layout, logFilename, false);
      logger.addAppender(jobFileLog);
    } catch (IOException e) {
      logger = Logger.getLogger(loggerClass);
      logger.error("Failed to open per-job log file (" + logFilename + ")");
    }
    if (resource != null)
      setIdentify("JOB(" + resource.getId() + " " + resource.getName() + "): ");
    		//"" STORAGE#" + (resource.getStorage() != null? resource.getStorage().getId() : ""));
  }

  public void setIdentify(String identify) {
    this.identify = identify;
  }

  public String getIdentify() {
    return identify;
  }

  void debug(StackTraceElement[] stackTrace) {
    for (int index = 0 ; index < stackTrace.length; index++)
      logger.debug( getIdentify() + " " + stackTrace[index].toString());
  }

  public void debug(String msg) {
    logger.debug( getIdentify() + " " + msg);
  }

  public void warnIfNotExpectedResponse(String actual, String expected) {
    if (actual.indexOf(expected) < 0) {
      logger.warn(getIdentify() + " Unexpected response '" + actual + "' does not contain '" + expected + "'");
    }
  }

  public void warn(String msg) {
    logger.warn( getIdentify() + " " + msg);
  }

  public void warn(String msg, Throwable t) {
    logger.warn( getIdentify() + " " + msg, t);
  }

  public void info(String msg) {
    logger.info(getIdentify() + " " + msg);
  }

  public void error(String msg) {
    logger.error(getIdentify() + " " + msg);
  }

  public void fatal(String msg) {
    logger.fatal(getIdentify() + " " + msg);
    // System.exit(1);
  }

  @Override
  public void debug(String msg, Throwable t) {
    logger.debug( getIdentify() + " " + msg, t);
  }

  @Override
  public void info(String msg, Throwable t) {
    logger.info( getIdentify() + " " + msg, t);
  }

  @Override
  public void error(String msg, Throwable t) {
    logger.error( getIdentify() + " " + msg, t);
  }

  @Override
  public void fatal(String msg, Throwable t) {
    logger.fatal( getIdentify() + " " + msg, t);
  }

  @Override
  public void log(Level level, String msg) {
    logger.log( level, getIdentify() + " " + msg);
  }

  @Override
  public void log(Level level, String msg, Throwable t) {
    logger.log( level, getIdentify() + " " + msg, t);
  }

  @Override
  public void log(Level level, Throwable t) {
    logger.log( level, getIdentify(), t);
  }
}
