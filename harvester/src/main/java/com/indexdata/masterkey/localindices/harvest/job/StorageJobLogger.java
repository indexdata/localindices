package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.util.HarvestableLog;


public abstract class StorageJobLogger implements LocalIndicesLogger {

  protected Logger logger; 
  private String identify;
  Layout layout = new PatternLayout("%d %-5p %m\n");
  Appender logAppender  = null;   

  public StorageJobLogger(Class<? extends Object> loggerClass, Storage resource) {
    
    String logId = "storage-" + (resource != null ? resource.getId() : "null");
    logger = Logger.getLogger(logId);
    String logFilename = "/var/log/masterkey/harvester/" + logId  + ".log";
    setupAppender(loggerClass,logFilename, "storage");
    logger.setAdditivity(false);
    if (resource != null)
      setIdentify("STORAGE(" + resource.getId() + " " + resource.getName() + "): ");
  }

  public StorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    String logFilename = HarvestableLog.getHarvesteableJobFilename(resource.getId());
    logger = Logger.getLogger(loggerClass.getName() + "JOB#" + resource.getId() );
    setupAppender(loggerClass, logFilename, "job");
    logger.setAdditivity(false);
    if (resource != null)
      setIdentify("JOB(" + resource.getId() + " " + resource.getName() + "): ");
    		//"" STORAGE#" + (resource.getStorage() != null? resource.getStorage().getId() : ""));
  }

  protected abstract void setupAppender(Class<? extends Object> loggerClass, String logFilename, String type);
  
  public void addAppender(Appender logAppender) 
  {
    if (logger.getAppender(logAppender.getName()) == null)
	logger.addAppender(logAppender);
  }

  public void removeAppender(Appender logAppender) {
    logger.removeAppender(logAppender);
  }

  public void setIdentify(String identify) {
    this.identify = identify;
  }

  public String getIdentify() {
    return identify;
  }

  void debug(StackTraceElement[] stackTrace) {
    for (int index = 0 ; index < stackTrace.length; index++)
      logger.log(StorageJobLogger.class.getCanonicalName(), Level.DEBUG, getIdentify() + " " + stackTrace[index].toString(), null);
  }

  public void debug(String msg) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.DEBUG, getIdentify() + " " + msg, null);
  }

  public void warnIfNotExpectedResponse(String actual, String expected) {
    if (actual.indexOf(expected) < 0) {
      logger.log(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + " Unexpected response '" + actual + "' does not contain '" + expected + "'", null);
    }
  }

  public void warn(String msg) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + " " + msg, null);
  }

  public void warn(String msg, Throwable t) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + " " + msg, t);
  }

  public void info(String msg) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.INFO, getIdentify() + " " + msg, null);
  }

  public void error(String msg) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.ERROR, getIdentify() + " " + msg, null);
  }

  public void fatal(String msg) {
    logger.log(StorageJobLogger.class.getCanonicalName(), Level.FATAL, getIdentify() + " " + msg, null);
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
  
  public void close() {
    if (logAppender != null)
      logger.removeAppender(logAppender);
  }

  protected Logger getLogger() {
    return logger;
  }

  protected void setLogger(Logger logger) {
    this.logger = logger;
  }
}
