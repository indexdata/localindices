package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionRecordError implements RecordError {

    public String exceptionType;
    public String message;
    public String stackTrace;
    public String context;
    public String typeOfEntity;

    public ExceptionRecordError(Exception e, String context, String typeOfEntity) {
      this.exceptionType = e.getClass().getSimpleName();
      this.message = e.getLocalizedMessage();
      this.stackTrace = stackTraceAsString(e);
      this.context = context==null ? "" : context;
      this.typeOfEntity = typeOfEntity;
    }

    public ExceptionRecordError(Exception e, String context) {
        this(e, context, "unspecified");
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
    public String getMessage() {
        return context + "; [" + exceptionType + "]; " + message;
    }

    public String getErrorContext() {
      return context;
    }

    public String getType() {
      return exceptionType;
    }

    public String getBriefMessage() {
      return message;
    }

    public String getCountingKey () {
      return message;
    }

    public String getStorageEntity() {
      return typeOfEntity;
    }

}