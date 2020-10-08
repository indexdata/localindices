package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import org.apache.http.StatusLine;

public class HttpRecordError implements RecordError {

    public int statusCode;
    public String reasonPhrase;
    public String serverMessage;
    public String additionalContext;
    public String recordType;
    public String countingMessage;
    public String transaction;
    public String entity;

    public HttpRecordError(int statusCode, String reasonPhrase, String serverMessage, String countingMessage, String additionalContext, String recordType, String transaction, String entity) {
      this.statusCode = statusCode;
      this.reasonPhrase = reasonPhrase;
      this.serverMessage = serverMessage;
      this.additionalContext = additionalContext;
      this.recordType = recordType;
      this.countingMessage = countingMessage;
      this.transaction = transaction;
      this.entity = entity;
    }

    public HttpRecordError(StatusLine httpStatus, String serverMessage, String countingMessage, String additionalContext, String recordType, String transaction, String entity) {
      this(httpStatus.getStatusCode(), httpStatus.getReasonPhrase(), serverMessage, countingMessage, additionalContext, recordType, transaction, entity);
    }

    @Override
    public String toString() {
      return recordType + ": " + additionalContext + ". Status code ["+statusCode+"]." + reasonPhrase + "]." + serverMessage;
    }

    @Override
    public String getMessageWithContext() {
      return additionalContext + "; " + reasonPhrase + "; " + serverMessage;
    }

    @Override
    public String getAdditionalContext() {
      return additionalContext;
    }

    @Override
    public String getErrorType() {
      return reasonPhrase;
    }

    @Override
    public String getServerMessage() {
      return serverMessage;
    }

    @Override
    public String getShortMessageForCounting() {
      return countingMessage;
    }

    @Override
    public String getTransaction() {
        return transaction;
    }

    @Override
    public String getEntity() {
        return entity;
    }

    @Override
    public String getRecordType() {
      return recordType;
    }
}
