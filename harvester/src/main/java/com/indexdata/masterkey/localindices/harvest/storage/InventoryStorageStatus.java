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
  String okapiURL;
  String accessToken;
  TransactionState transactionState = TransactionState.NoTransaction;

  public InventoryStorageStatus(String okapiURL, String accessToken) {
    adds = 0L;
    deletes = 0L;
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
    this.transactionState = state;
  }

  @Override
  public Long getOutstandingAdds() {
    // TODO: Support pseudo-transactions
    return 0L;
  }

  @Override
  public Long getOutstandingDeletes() {
    // TODO: Support pseudo-transactions
    return 0L;
  }

  @Override
  public Long getAdds() {
    return deletes;
  }

  @Override
  public Long getDeletes() {
    return adds;
  }

}
