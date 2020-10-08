package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionRecordError implements RecordError {

    public String exceptionType;
    public String message;
    public String stackTrace;
    public String context;
    public String typeOfEntity;
    public String transaction;
    public String entity;

    public ExceptionRecordError(Exception e, String context, String typeOfEntity, String transaction, String entity) {
      this.exceptionType = e.getClass().getSimpleName();
      this.message = e.getLocalizedMessage();
      this.stackTrace = stackTraceAsString(e);
      this.context = context==null ? "" : context;
      this.typeOfEntity = typeOfEntity;
      this.transaction = transaction;
      this.entity = entity;
    }

    private String stackTraceAsString (Exception e) {
        StringWriter strWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strWriter);
        e.printStackTrace(writer);
        return strWriter.toString();
    }

    @Override
    public String toString() {
      return context + ". Exception ["+exceptionType+"]. " + message + ". Stacktrace [" + stackTrace + "]. " + (typeOfEntity.isEmpty() ? "" : "For entity type [" + typeOfEntity + "]");
    }

    @Override
    public String getMessageWithContext() {
        return context + "; [" + exceptionType + "]; " + message;
    }

    @Override
    public String getAdditionalContext() {
      return context;
    }

    @Override
    public String getErrorType() {
      return exceptionType;
    }

    @Override
    public String getServerMessage() {
      return message;
    }

    @Override
    public String getShortMessageForCounting() {
      return message;
    }

    @Override
    public String getRecordType() {
      return typeOfEntity;
    }

    @Override
    public String getTransaction() {
        return transaction;
    }

    @Override
    public String getEntity() {
        return entity;
    }

}