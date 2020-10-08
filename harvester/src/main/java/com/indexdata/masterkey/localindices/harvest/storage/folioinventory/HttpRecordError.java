package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import org.apache.http.StatusLine;

public class HttpRecordError implements RecordError {

    public int statusCode;
    public String reason;
    public String response;
    public String context;
    public String entity;
    public String countingKey;

    public HttpRecordError(int status, String reason, String response, String countingKey, String context, String entity) {
      this.statusCode = status;
      this.reason = reason;
      this.response = response;
      this.context = context;
      this.entity = entity;
      this.countingKey = countingKey;
    }

    public HttpRecordError(StatusLine httpStatus, String response, String countingKey, String context, String entity) {
      this(httpStatus.getStatusCode(), httpStatus.getReasonPhrase(), response, countingKey, context, entity);
    }

    @Override
    public String toString() {
      return entity + ": " + context + ". Status code ["+statusCode+"]." + reason + "]." + response;
    }

    @Override
    public String getMessage() {
      return context + "; " + reason + "; " + response;
    }

    @Override
    public String getErrorContext() {
      return context;
    }

    @Override
    public String getType() {
      return reason;
    }

    @Override
    public String getBriefMessage() {
      return response;
    }

    @Override
    public String getCountingKey() {
      return countingKey;
    }

    @Override
    public String getStorageEntity() {
      return entity;
    }
}
