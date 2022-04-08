package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.*;

import org.apache.commons.net.ftp.FTPClient;

public class FtpInputStream extends InputStream {
  @SuppressWarnings("unused")
  private final StorageJobLogger logger;
  FTPClient client;
  InputStream input;
  long length;
  // For logging long-running FTP operations as timeouts can cause issues to debug
  long startTime;
  private static long ASSUMED_IDLING_TIMEOUT_MS = 900*1000;
  // With long-running FTP file reads, the server can time out in certain ways that
  // makes the completePendingCommand hang seemingly indefinitely. The typical server
  // time out seems to be 900 seconds (15 minutes). As no other way has been identified
  // to safely diagnose this condition, the completePendingCommand has been wrapped in
  // a future that will be timed out eventually. Timing it out too soon seems to
  // potentially lead to premature end of file for the current stream. The timeout is
  // set to 1 minute.
  private final long COMPLETE_PENDING_COMMAND_TIMEOUT_MINUTES = 1;

  public FtpInputStream(InputStream data, long length, FTPClient client, StorageJobLogger logger) {
    input = data;
    this.client = client;
    this.length = length;
    this.logger = logger;
    this.startTime = System.currentTimeMillis();
  }

  @Override
  public int read() throws IOException {
    try {
      length--;
      return input.read();
    } catch (IOException ioe) {
      logger.debug("FtpInputStream.read() encountered IO exception");
      // TODO map to EOF
      if (length <= 0)
        throw new EOFException("All bytes read");
      throw ioe;
    }
  }

  @Override
  public void close() throws IOException {
    long elapsed = elapsed();
    logger.debug("Closing input");
    input.close();
    ExecutorService executor = Executors.newCachedThreadPool();
    Callable<Object> task = new Callable<Object>()  {
      public Object call() {
        try {
          return client.completePendingCommand();
        } catch (IOException ioe) {
          logger.warn("CompletePendingCommand did not complete "+ ioe.getMessage());
          return false;
        }
      }
    };
    Future<Object> future = executor.submit(task);

    try {
      if (elapsed>ASSUMED_IDLING_TIMEOUT_MS) {
        logger.info("Calling CompletePendingCommand but FTP server may have timed out operation after " + (elapsed / 1000) + " seconds");
      }
      Boolean result = (Boolean) future.get(COMPLETE_PENDING_COMMAND_TIMEOUT_MINUTES, TimeUnit.MINUTES);
      if (!result)  {
        throw new IOException("CompletePendingCommand timed out");
      }
    } catch (TimeoutException ex) {
      if (elapsed>ASSUMED_IDLING_TIMEOUT_MS) {
        logger.info("CompletePendingCommand did not return in " + COMPLETE_PENDING_COMMAND_TIMEOUT_MINUTES + " minutes. Cancelled, disconnecting.");
      } else {
        logger.warn("CompletePendingCommand did not return in " + COMPLETE_PENDING_COMMAND_TIMEOUT_MINUTES + " minutes. Cancelled, disconnecting.");
      }
      client.disconnect();
    } catch (InterruptedException e) {
      logger.error("Invocation of client.completePendingCommand interrupted: " + e.getMessage());
    } catch (ExecutionException e) {
      logger.error("Error executing client.completePendingCommand: " + e.getMessage());
    } finally {
      future.cancel(true);
    }
  }

  private long elapsed() {
    return System.currentTimeMillis()-startTime;
  }
}
