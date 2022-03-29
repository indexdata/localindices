package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class FolioRecordUpdater {
  protected StorageJobLogger logger;

  abstract void addRecord (RecordJSON recordJSON);
  abstract void deleteRecord (RecordJSON recordJSON);

  protected JSONObject getResponseAsJson(String responseAsString) {
    JSONObject upsertResponse;
    JSONParser parser = new JSONParser();
    try {
      upsertResponse = (JSONObject) parser.parse(responseAsString);
    } catch ( ParseException pe) {
      upsertResponse = new JSONObject();
      upsertResponse.put("wrappedErrorMessage", responseAsString);
    }
    return upsertResponse;
  }


}
