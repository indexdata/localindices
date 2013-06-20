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
    return status.getAdds().equals(getAdds()) && 
	   status.getDeletes().equals(getDeletes()) && 
	   status.getTotalRecords().equals(getTotalRecords());
  }
  
}
