package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;

public class FtpInputStream extends InputStream {
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
      //TODO map to EOF
      if (length <= 0)
	throw new EOFException("All bytes read");
      throw ioe;
    }
  }

  @Override
  public void close() throws IOException {
    input.close();
    if (!client.completePendingCommand())
      throw new IOException("Failed to complete FTP InputStream close()");
  }
}
