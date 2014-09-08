package com.indexdata.masterkey.localindices.client;

import static com.indexdata.masterkey.localindices.client.RemoteFile.basename;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FtpRemoteFileIterator implements RemoteFileIterator {
  private final StorageJobLogger logger;
  private final FtpClientTransport transport;
  private final URL parent;
  private final List<FTPFile> files = new LinkedList<FTPFile>();
  private FTPFileFilter filter = null;
  private int index = 0;
  
  public FtpRemoteFileIterator(FtpClientTransport transport, URL parent, FTPFile[] files, FTPFileFilter filter,
    StorageJobLogger logger) {
    this.transport = transport;
    this.parent = parent;
    this.logger = logger;
    this.filter = filter;
    for (FTPFile file : files)
      if (!(basename(file.getName()).equals(".") || basename(file.getName()).equals(".."))) {
        this.files.add(file);
      }
  }
  
  @Override
  public boolean hasNext() throws IOException {
    return index < files.size();
  }

  @Override
  public synchronized RemoteFile getNext() throws IOException {
    FTPFile file = files.get(index++);   
    if (file.isDirectory()) {
      return new FtpRemoteDirectory(parent, transport, file, filter, logger);
    }
    else 
      return new FtpRemoteFile(parent, file, transport, logger);
  }

}
