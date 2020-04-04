/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.ArrayList;
import java.util.List;

import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;

/**
 *
 * @author ne
 */
public class RecordErrors {

  RecordJSON record;
  List<String> errorMessages = new ArrayList<String>();

  RecordErrors(RecordJSON recordJson) {
    this.record = recordJson;
  }

  void addErrorMessage(String msg) {
    errorMessages.add(msg);
  }

  boolean hasErrors () {
    return errorMessages.size()>0;
  }

}
