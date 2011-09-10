package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Level;

public interface LocalIndicesLogger {

  //void trace(String msg);
  void debug(String msg);
  void debug(String msg, Throwable t);
  void info(String msg);
  void info(String msg, Throwable t);
  void warn(String msg);
  void warn(String msg, Throwable t);;
  void error(String msg);
  void error(String msg, Throwable t);
  void fatal(String msg);
  void fatal(String msg, Throwable t);
  
  void log(Level level, String msg);
  void log(Level level, String msg, Throwable t);
  void log(Level level, Throwable t);

}
