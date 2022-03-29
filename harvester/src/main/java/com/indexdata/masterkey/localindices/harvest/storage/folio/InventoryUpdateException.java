package com.indexdata.masterkey.localindices.harvest.storage.folio;

public class InventoryUpdateException extends Exception {

    private static final long serialVersionUID = -6566607704423952098L;

    public InventoryUpdateException(String message) {
      super(message);
    }

    public InventoryUpdateException(String message, Throwable e) {
      super(message,e);
    }
}