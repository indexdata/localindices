/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

/**
 *
 * @author kurt
 */
public class InventoryStorageStatus extends AbstractStorageStatus {

  long adds;
  long deletes;

  long outstandingAdds;
  long outstandingDeletes;

  String okapiURL;
  String accessToken;
  TransactionState transactionState = TransactionState.NoTransaction;

  public InventoryStorageStatus(String okapiURL, String accessToken) {
    adds = 0L;
    deletes = 0L;
    outstandingAdds = 0L;
    outstandingDeletes = 0L;

    this.okapiURL = okapiURL;
    this.accessToken = accessToken;
  }

  @Override
  public Long getTotalRecords() {
    //TODO: Query inventory instance to see how many records exist
    return 0L;
  }

  @Override
  public TransactionState getTransactionState() {
    return this.transactionState;
  }

  @Override
  public void setTransactionState(TransactionState state) {
    if(TransactionState.Committed == state) {
      adds = outstandingAdds;
      deletes = outstandingDeletes;
      outstandingAdds = 0;
      outstandingDeletes = 0;
    }
    this.transactionState = state;
  }

  @Override
  public Long getOutstandingAdds() {
    return outstandingAdds;
  }

  @Override
  public Long getOutstandingDeletes() {
    return outstandingDeletes;
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
    outstandingAdds += add;
    return outstandingAdds;
  }

  public synchronized long incrementDelete(long delete) {
    outstandingDeletes += delete;
    return outstandingDeletes;
  }

}
