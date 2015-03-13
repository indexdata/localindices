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
  private String identify = "";
  Layout layout = new PatternLayout("%d %-5p [%t] %m\n");
  Appender logAppender  = null;   

  public StorageJobLogger(Class<? extends Object> loggerClass, Storage resource) {
    String logId = "storage-" + (resource != null ? resource.getId() : "null");
    logger = Logger.getLogger(logId);
    String logFilename = "/var/log/masterkey/harvester/" + logId  + ".log";
    setupAppender(loggerClass,logFilename, "storage");
    /* 
    Appender appender = Logger.getRootLogger().getAppender("LOGFILE");
    if (appender != null) {
      addAppender(appender);
    }
    */
    logger.setAdditivity(false);
    logger.setLevel(Level.DEBUG);
    if (resource != null)
      setIdentify("STORAGE(" + resource.getId() + " " + resource.getName() + "): ");
  }

  public StorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    String logFilename = "/var/log/masterkey/harvester/job-" + resource.getId() + ".log";
    logger = Logger.getLogger(loggerClass.getName() + "JOB#" + resource.getId() );
    /*
    Appender appender = Logger.getRootLogger().getAppender("LOGFILE");
    if (appender != null) {
      addAppender(appender);
    }
    */
    setupAppender(loggerClass, logFilename, "job");
    logger.setLevel(Level.INFO);
    if (resource.getLogLevel() != null) {
      Level level = Level.toLevel(resource.getLogLevel());
      logger.setLevel(level);
    }
    
    logger.setAdditivity(false);
/*
    // Configured in the thread name
    if (resource != null)
      setIdentify("JOB(" + resource.getId() + " " + resource.getName() + "): ");
*/
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
    for (int index = 0; index < stackTrace.length; index++)
      doLog(StorageJobLogger.class.getCanonicalName(), Level.DEBUG, getIdentify() + stackTrace[index].toString(), null);
  }

  public void debug(String msg) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.DEBUG, getIdentify() + msg, null);
  }

  public void warnIfNotExpectedResponse(String actual, String expected) {
    if (actual.indexOf(expected) < 0) {
      doLog(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + "Unexpected response '" + actual + "' does not contain '" + expected + "'", null);
    }
  }
  
  public void warn(String msg) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + msg, null);
  }

  public void warn(String msg, Throwable t) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.WARN, getIdentify() + msg, t);
  }

  public void info(String msg) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.INFO, getIdentify() + msg, null);
  }

  public void error(String msg) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.ERROR, getIdentify() + msg, null);
  }

  public void fatal(String msg) {
    doLog(StorageJobLogger.class.getCanonicalName(), Level.FATAL, getIdentify() + msg, null);
  }

  @Override
  public void debug(String msg, Throwable t) {
    doLog(Level.DEBUG, getIdentify() + msg, t);
  }

  @Override
  public void info(String msg, Throwable t) {
    doLog(Level.INFO, getIdentify() + msg, t);
  }

  @Override
  public void error(String msg, Throwable t) {
    doLog(Level.ERROR, getIdentify() + msg, t);
  }

  @Override
  public void fatal(String msg, Throwable t) {
    doLog(Level.FATAL, getIdentify() + msg, t);
  }

  @Override
  public void log(Level level, String msg) {
    doLog(level, getIdentify() + msg);
  }

  @Override
  public void log(Level level, String msg, Throwable t) {
    doLog(level, getIdentify() + msg, t);
  }

  @Override
  public void log(Level level, Throwable t) {
    doLog(level, getIdentify(), t);
  }
  
  private void doLog(String callerFQCN, Level level, Object message, Throwable t) {
    try {
      logger.log(callerFQCN, level, message, t);
    } catch (NullPointerException npe) {
      printLogMessageOnNPE(message);
    }
  }

  private void doLog(Level level, Object message, Throwable t) {
    try {
      logger.log(level, message, t);
    } catch (NullPointerException npe) {
      printLogMessageOnNPE(message);
    }
  }
  
  private void doLog(Level level, String msg) {
    try {
      logger.log( level, msg);
    } catch (NullPointerException npe) {
      printLogMessageOnNPE(msg);
    }
  }

  private void printLogMessageOnNPE (Object message) {
    System.out.println("* This NullPointerException occurred while attempting to log this message: \n" +
                       "*   \"" + message + "\"\n" +
                       "* (The NPE itself was likely due to the context being reloaded while logging.)");
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
