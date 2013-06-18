/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

/**
 *
 * @author dennis
 */
public abstract class AbstractStorageStatus implements StorageStatus {

  @Override
  public boolean equals(StorageStatus status) {
    return status.getAdds() == getAdds() && status.getDeletes() == getAdds() && status.getTotalRecords() == getTotalRecords();
  }
  
}
