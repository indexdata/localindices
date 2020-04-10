package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import org.apache.http.StatusLine;

public class HttpRecordError implements RecordError {

    public int statusCode;
    public String reason;
    public String response;
    public String shortDescription;
    public String entity;

    public HttpRecordError(int status, String reason, String response, String shortDescription, String entity) {
      this.statusCode = status;
      this.reason = reason;
      this.response = response;
      this.shortDescription = shortDescription;
      this.entity = entity;
    }

    public HttpRecordError(int status, String reason, String response, String shortDescription) {
      this(status, reason, response, shortDescription, "unspecified");
    }

    public HttpRecordError(StatusLine httpStatus, String response, String additionalDescription, String entity) {
      this(httpStatus.getStatusCode(), httpStatus.getReasonPhrase(), response, additionalDescription, entity);
    }

    @Override
    public String toString() {
      return "Description [" + shortDescription + "] Status code ["+statusCode+"]. Reason [" + reason + "]. Description [" + response + "]. " + (entity.isEmpty() ? "" : "For entity type [" + entity + "]");
    }

    @Override
    public String briefMessage() {
      return shortDescription + " " + reason + " " + response;
    }
}
