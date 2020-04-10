package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionRecordError implements RecordError {

    public String exceptionType;
    public String message;
    public String stackTrace;
    public String shortDescription;
    public String typeOfEntity;

    public ExceptionRecordError(Exception e, String shortDescription, String typeOfEntity) {
      this.exceptionType = e.getClass().getSimpleName();
      this.message = e.getLocalizedMessage();
      this.stackTrace = stackTraceAsString(e);
      this.shortDescription = shortDescription==null ? "" : shortDescription;
      this.typeOfEntity = typeOfEntity;
    }

    public ExceptionRecordError(Exception e, String shortDescription) {
        this(e, shortDescription, "unspecified");
    }

    private String stackTraceAsString (Exception e) {
        StringWriter strWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strWriter);
        e.printStackTrace(writer);
        return strWriter.toString();
    }

    @Override
    public String toString() {
      return "Description [" + shortDescription + "] Exception ["+exceptionType+"]. Message [" + message + "]. Stacktrace [" + stackTrace + "]. " + (typeOfEntity.isEmpty() ? "" : "For entity type [" + typeOfEntity + "]");
    }

    @Override
    public String briefMessage() {
        return shortDescription + " [" + exceptionType + "]: " + message;
    }

}