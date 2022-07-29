/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folio;

/**
 * Holds counts of records processed, updated, deleted, failed
 * @author ne
 */
public class InventoryRecordUpdateCounters {

  protected int xmlBulkRecordsSkipped = 0;

  protected int instancesProcessed = 0;
  protected int instancesLoaded = 0;
  protected int instanceDeleteSignals = 0;
  protected int instanceDeletions = 0;
  protected int instancesFailed = 0;

  protected int holdingsRecordsProcessed = 0;
  protected int holdingsRecordsLoaded = 0;
  protected int holdingsRecordsDeleted = 0;
  protected int holdingsRecordsFailed = 0;

  protected int itemsProcessed = 0;
  protected int itemsLoaded = 0;
  protected int itemsDeleted = 0;
  protected int itemsFailed = 0;

  protected int sourceRecordsProcessed = 0;
  protected int sourceRecordsLoaded = 0;
  protected int sourceRecordsDeleted = 0;
  protected int sourceRecordsFailed = 0;

  protected int lastLogOfInstancesProcessed = 0;

}
