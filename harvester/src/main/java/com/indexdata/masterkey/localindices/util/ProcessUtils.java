/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */
package com.indexdata.masterkey.localindices.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Process helper class.
 * 
 * @author jakub
 */
public class ProcessUtils {

  /**
   * Executes a process and returns immediately.
   * 
   * @param cmd
   *          a command to be executed
   * @return executed process
   * @throws java.io.IOException
   */
  public static Process execAndReturn(String[] cmd) throws IOException {
    Process proc = Runtime.getRuntime().exec(cmd);
    return proc;
  }

  /**
   * Executes a given command starts a new process, logs it's output and
   * destroys it when tha calling thread is interrupted.
   * 
   * @param cmd
   *          command to be executed
   * @param logger
   *          a logger to log to
   * @return process return value
   * @throws java.io.IOException
   */
  public static int execAndWait(String[] cmd, Logger logger) throws IOException {
    if (logger.isDebugEnabled()) {
      String logcmd = "About to execute command: ";
      for (String c : cmd) {
	logcmd += c + " ";
      }
      logger.log(Level.DEBUG, logcmd);
    }
    Process p = execAndReturn(cmd);
    logError(p, logger);
    try {
      p.waitFor();
      return p.exitValue();
    } catch (InterruptedException ie) {
      logger.log(Level.WARN, "Calling thread was interupted, destroying the process.");
      p.destroy();
      return -1;
    }
  }

  /**
   * Logs the process error output in a seperate thread.
   * 
   * @param proc
   *          process that prints the output to be logged
   * @param logger
   *          a logger to log to
   * @return logging thread
   */
  public static Thread logError(final Process proc, final Logger logger) {
    Thread lT = new Thread() {

      @Override
      public void run() {
	InputStream is = proc.getErrorStream();
	InputStreamReader isr = new InputStreamReader(is);
	BufferedReader br = new BufferedReader(isr);
	String line;
	try {
	  while ((line = br.readLine()) != null) {
	    logger.log(Level.INFO, line);
	  }
	} catch (IOException ex) {
	  logger.log(Level.WARN,
	      "Logging thread encountered an io error (process died?) and will exit.");
	}
      }
    };
    lT.start();
    return lT;
  }
}
