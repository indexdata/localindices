package com.indexdata.masterkey.localindices.harvest.storage;

public class SimpleStorageStatus extends AbstractStorageStatus {

  Long adds;
  Long deletes;
  Long total;
  TransactionState state = TransactionState.NoTransaction;

  public SimpleStorageStatus(long adds, long deletes, boolean committed) {
    this.adds = adds;
    this.deletes = deletes;
    this.total = null;
    this.state = (committed ? TransactionState.Committed : TransactionState.InTransaction);
  }

  public SimpleStorageStatus(long adds, long deletes, boolean committed, Long total) {
    this.adds = adds;
    this.deletes = deletes;
    this.total = total;
    this.state = (committed ? TransactionState.Committed : TransactionState.InTransaction);
  }

  public SimpleStorageStatus(StorageStatus status) {
    this.adds = status.getAdds();
    this.deletes = status.getDeletes();
    this.total = status.getTotalRecords();
    this.state = status.getTransactionState();
  }

  @Override
  public Long getTotalRecords() {
    if (total == null)
      return adds - deletes;
    else 
      return total;
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

  @Override
  public void setTransactionState(TransactionState state) {
    this.state = state;
  }
}
