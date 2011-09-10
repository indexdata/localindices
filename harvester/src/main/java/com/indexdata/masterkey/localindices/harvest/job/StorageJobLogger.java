package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Harvestable;


public class StorageJobLogger implements LocalIndicesLogger {

  private Logger logger; 
  private String identify;
  
  public StorageJobLogger(Class<? extends Object> loggerClass, Harvestable resource) {
    logger = Logger.getLogger(loggerClass);
    setIdentify("JOB#" + resource.getId() + " STORAGE#" + (resource.getStorage() != null? resource.getStorage().getId() : ""));
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
      logger.warn(getIdentify() + " Unexpected response from Solr: '" + actual + "' does not contain '" + expected + "'");
    }
  }

  public void warn(String msg) {
    logger.warn( getIdentify() + " " + msg);
  }

  public void warn(String msg, Throwable t) {
    logger.warn( getIdentify() + " " + msg);
  }

  public void info(String msg) {
    logger.info(getIdentify() + msg);
  }

  public void error(String msg) {
    logger.error(getIdentify() + msg);
  }

  public void error(String msg, Exception e) {
    logger.error(getIdentify() + msg);
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
