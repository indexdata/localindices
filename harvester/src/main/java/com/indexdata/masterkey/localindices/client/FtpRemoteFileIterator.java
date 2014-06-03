package com.indexdata.masterkey.localindices.client;

import static com.indexdata.masterkey.localindices.client.RemoteFile.basename;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteFileIterator implements RemoteFileIterator {
  private final StorageJobLogger logger;
  private final FTPClient client;
  private final URL parent;
  private final List<FTPFile> files = new LinkedList<FTPFile>();
  private int index = 0;
  
  public FtpRemoteFileIterator(FTPClient client, URL parent, FTPFile[] files, 
    StorageJobLogger logger) {
    this.client = client;
    this.parent = parent;
    this.logger = logger;
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
      return new FtpRemoteDirectory(parent, file, client, logger);
    }
    else 
      return new FtpRemoteFile(parent, file, client, logger);
  }

}
