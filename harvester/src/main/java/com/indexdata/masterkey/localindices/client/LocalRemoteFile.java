package com.indexdata.masterkey.localindices.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class LocalRemoteFile extends RemoteFile {

  /**
   * 
   */
  private static final long serialVersionUID = -7744657605686899762L;

  public LocalRemoteFile(File file) throws MalformedURLException, FileNotFoundException, URISyntaxException {
    super(new URL("file:://" + file.getAbsolutePath()), new FileInputStream(file), false); 
  }

}
