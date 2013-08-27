/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

/**
 *
 * @author Dennis
 */
public abstract class AbstractStorageStatus implements StorageStatus {

  @Override
  public boolean equals(StorageStatus status) {
    return status.getAdds().equals(getAdds()) && 
	   status.getDeletes().equals(getDeletes()) && 
	   status.getTotalRecords().equals(getTotalRecords());
  }

  public String toString() {
    StringBuffer result = new StringBuffer("Adds: ");
    result.append(getAdds()).append(" Deletes: ").append(getDeletes()).append(" Total: ").append(getTotalRecords());
    return result.toString();
  }
}
