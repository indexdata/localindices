package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;

public class FtpInputStream extends InputStream {
  @SuppressWarnings("unused")
  private final StorageJobLogger logger;
  FTPClient client;
  InputStream input;
  long length;

  public FtpInputStream(InputStream data, long length, FTPClient client, StorageJobLogger logger) {
    input = data;
    this.client = client;
    this.length = length;
    this.logger = logger;
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
    logger.debug("FtpInputStream closing FTP input stream");
    input.close();
    logger.debug("Closed FTP input stream");
    /*
    if (!client.completePendingCommand()) {
      logger.error("FTP didn't close properly it seems. Logging out and disconnecting");
      client.logout();
      client.disconnect();
      logger.debug("Logged out and disconnected");
      // throw new IOException("Failed to complete FTP InputStream close()");
    } else {
      logger.debug("FtpInputStream confirmed FTP stream closed");
    }
    */
  }
}
