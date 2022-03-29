package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.harvest.storage.AbstractStorageStatus;

/**
 *
 * @author kurt
 */
public class FolioStorageStatus extends AbstractStorageStatus {

  long adds;
  long deletes;
  TransactionState transactionState = TransactionState.NoTransaction;

  public FolioStorageStatus() {
    adds = 0L;
    deletes = 0L;
  }

  @Override
  public Long getTotalRecords() {
    return 0L;
  }

  @Override
  public TransactionState getTransactionState() {
    return this.transactionState;
  }

  @Override
  public void setTransactionState(TransactionState state) {
    this.transactionState = state;
  }

  @Override
  public Long getOutstandingAdds() {
    return 0L;
  }

  @Override
  public Long getOutstandingDeletes() {
    return 0L;
  }

  @Override
  public Long getAdds() {
    return adds;
  }

  @Override
  public Long getDeletes() {
    return deletes;
  }

  public synchronized long incrementAdd(long add) {
    adds += add;
    return adds;
  }

  public synchronized long incrementDelete(long delete) {
    deletes += delete;
    return deletes;
  }

}
