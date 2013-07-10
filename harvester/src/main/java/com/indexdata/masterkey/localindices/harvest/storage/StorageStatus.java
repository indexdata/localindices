package com.indexdata.masterkey.localindices.harvest.storage;

public interface StorageStatus {
  /* Returns null on not implemented */
  enum TransactionState 
  	{
    		NoTransaction,
    		InTransaction, 
    		Committed
  }
  Long getTotalRecords();
  TransactionState getTransactionState();
  Long getOutstandingAdds();
  Long getOutstandingDeletes();
  Long getAdds();
  Long getDeletes();
  boolean equals(StorageStatus status);
}
