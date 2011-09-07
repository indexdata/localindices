package com.indexdata.masterkey.localindices.harvest.storage.backend;

import java.util.Properties;

public interface StorageBackend {

  void init(Properties prop);

  int start();

  int stop();

  boolean isRunning();
  // StorageClient getClient();
}
