package com.indexdata.masterkey.localindices.harvest.storage;

public class SimpleStorageStatus implements StorageStatus {

  Long adds;
  Long deletes;
  TransactionState state = TransactionState.NoTransaction;

  public SimpleStorageStatus(long adds, long deletes, boolean committed) {
    this.adds = adds;
    this.deletes = deletes;
    this.state = (committed ? TransactionState.Committed : TransactionState.InTransaction);
    
  }
  @Override
  public Long getTotalRecords() {
    
    return adds - deletes;
  }

  @Override
  public Long getOutstandingAdds() {
    if (state == TransactionState.InTransaction)
      return adds;
    return null;
  }

  @Override
  public Long getOutstandingDeletes() {
    if (state == TransactionState.InTransaction)
      return deletes;
    return null;
  }

  @Override
  public Long getAdds() {
    if (state == TransactionState.Committed)
      return adds;
    return null;
  }

  @Override
  public Long getDeletes() {
    if (state == TransactionState.Committed)
      return deletes;
    return null;
  }
  @Override
  public TransactionState getTransactionState() {
    return state;
  }

}
