package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;

public abstract class FolioRecordUpdater {
  protected StorageJobLogger logger;

  abstract void addRecord (RecordJSON recordJSON);
  abstract void deleteRecord (RecordJSON recordJSON);
  abstract void releaseBatch ();

}
