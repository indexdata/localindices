/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;

import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.cliftonlabs.json_simple.Jsoner;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.utils.XmlUtils;

/**
 * Holds errors encountered in the processing of one Inventory record set (the
 * Inventory records derived from one incoming bibliographic record)
 *
 * @author ne
 */
public class RecordWithErrors {

  RecordJSON record;
  List<RecordError> errors = new ArrayList<RecordError>();
  FailedRecordsController failCtrl;
  TransformedRecord recordProxy;
  StorageJobLogger logger;

  RecordWithErrors(RecordJSON recordJson, FailedRecordsController controller) {
    this.record = recordJson;
    this.failCtrl = controller;
    this.logger = controller.logger;
    this.recordProxy = new TransformedRecord(recordJson.toJson(),logger);
  }

  String getFileName () {
    return recordProxy.getLocalIdentifier() + ".xml";
  }

  String getFileName (int modifier) {
    return String.format("%s-%d.xml", recordProxy.getLocalIdentifier(), modifier);
  }

  @SuppressWarnings("unused")
  // implement to not overwrite potentially existing failed record for same local identifier
  private String timestamp () {
    LocalDateTime now = LocalDateTime.now();
    String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.getDefault()));
    return timestamp;
  }

  void addResponseError(HttpRecordError error) {
    errors.add(error);
  }

  void addException(ExceptionRecordError error) {
    errors.add(error);
  }

  void addError(RecordError error) {
    if (error instanceof HttpRecordError) {
      addResponseError((HttpRecordError)error);
    } else if (error instanceof ExceptionRecordError) {
      addException((ExceptionRecordError) error);
    }
  }

  void reportAndThrowError(RecordError error, Level logLevel) throws InventoryUpdateException {
    reportAndThrowError(error, logLevel, null);
  }

  void reportAndThrowError(RecordError error, Level logLevel, Throwable exception) throws InventoryUpdateException {
    addError(error);
    int count = failCtrl.incrementErrorCount(error);
    if (count <= 10) {
      logger.log(logLevel, error.getMessage());
    } else if (count>10 && count < 100) {
      logger.log(logLevel, error.getMessage());
    } else if (count % 100 == 0) {
      logger.error(String.format("%d records failed with %s", failCtrl.getErrorsByErrorMessage(error.getMessage()), error.getMessage()));
    }
    if (exception != null) {
      throw new InventoryUpdateException(error.toString(),exception);
    } else {
      throw new InventoryUpdateException(error.toString());
    }
  }

  void addResponseError(int status, String reason, String response, String message, String entity) {
    errors.add(new HttpRecordError(status, reason, response, message, entity));
  }

  void addResponseError(int status, String reason, String response, String message) {
    errors.add(new HttpRecordError(status, reason, response, message));
  }

  void addExceptionError(Exception e, String message, String entity) {
    errors.add(new ExceptionRecordError(e, message, entity));
  }

  void addExceptionError(Exception e, String message) {
    errors.add(new ExceptionRecordError(e, message, ""));
  }

  boolean hasErrors () {
    return errors.size()>0;
  }

  int numberOfErrors() {
    return errors.size();
  }

  /**
   * Log full error messages for errors that occurred for less than 10 records in the job so far
   * and a brief error message with a total count for every 100 records with only that error thereafter.
   *
   * @param logger
   * @param counters
   */
  void writeErrorsLog(StorageJobLogger logger) {
    if (hasErrors()) {
      int i=0;
      for (RecordError error : errors) {
        i++;
        int occurrences = failCtrl.getErrorsByErrorMessage(error.getMessage());
        if (occurrences < 10) {
          if (i==1) logger.error("Error" + (numberOfErrors() > 1 ? "s" : "") + " updating Inventory with  " + record.toJson().toJSONString());
          logger.error("#" + i + " " + error.getMessage());
        } else if (occurrences % 100 == 0) {
          logger.error(occurrences + " records have failed with " + error.getMessage());
        }
      }
    }
  }

  /**
   * Writes XML document for failed record with original record and diagnostics to file system
   */
  public void logFailedRecord () {
     failCtrl.saveFailedRecord(this);
  }

  /**
   * Retrieves original record from 1) the transformed record, 2) the stored original, 3) otherwise as null
   * @return byte array of the original record XML
   */
  private byte[] getOriginalRecord () {
    if (recordProxy.getOriginalXml() != null) {
      return recordProxy.getOriginalXml().getBytes();
    } else {
      if (record.getOriginalContent() != null) {
        return record.getOriginalContent();
      } else {
        logger.warn("No original record found, neither stored in Record with 'store original', nor passed through as element 'original' in transformed record XML/JSON");
        return null;
      }
    }
  }

  /**
   * Creates the XML document to store as a failed record. Contains the error message(s), the
   * original document if available and the transformed version.
   * @return
   */
  protected byte[] createFailedRecordXml () {
    byte[] failedRecordBytes = null;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      Document failedRecord = builder.parse(new InputSource(new StringReader("<failed-record></failed-record>")));

      addErrors(failedRecord);
      addOriginal(builder, failedRecord);
      addTransformedExcludingOriginal(failedRecord);

      Writer stringWriter = new StringWriter();

      Properties props = new Properties();
      props.put(OutputKeys.INDENT, "yes");
      props.put("{http://xml.apache.org/xslt}indent-amount", "2");
      XmlUtils.serialize(failedRecord, stringWriter, props);
      String failedRecordAsXmlString = stringWriter.toString();
      failedRecordBytes = failedRecordAsXmlString.getBytes();
    } catch (Exception e) {
      logger.error("Error creating failed record XML document: " + e.getMessage());
    }
    return failedRecordBytes;
  }

  /**
   * Clones the transformed record, removes the original record from clone and adds the clone as
   * a JSON string to the `original` element of the failed record XML
   * @param failedRecord
   */
  private void addTransformedExcludingOriginal(Document failedRecord) {

    Element transformedElement = failedRecord.createElement("transformed-record");
    JSONObject transformedExclOriginal = new JSONObject();

    JSONObject transformedJson = (JSONObject) record.toJson().get("record");
    if (transformedJson != null) {
      try {
        JSONParser parser = new JSONParser();
        transformedExclOriginal = (JSONObject) parser.parse(transformedJson.toJSONString());
        transformedExclOriginal.remove("original");
      } catch (ParseException e) {
        logger.error("Error creating transformed record excluding original record: " + e.getMessage());
      }
    }
    transformedElement.setTextContent(Jsoner.prettyPrint(transformedExclOriginal.toJSONString()));
    failedRecord.getDocumentElement().appendChild(transformedElement);
  }

  /**
   * Adds original record as XML to element `original` of the failed record XML
   * @param builder
   * @param failedRecord
   * @throws SAXException
   * @throws IOException
   */
  private void addOriginal(DocumentBuilder builder, Document failedRecord) throws SAXException, IOException {
    if (getOriginalRecord() != null) {
      String originalXml = new String(getOriginalRecord());
      Document originalRecord = builder.parse(new InputSource(new StringReader(originalXml)));
      Node originalRecordNode = failedRecord.importNode(originalRecord.getDocumentElement(),true);
      Element originalElement = failedRecord.createElement("original");
      originalElement.appendChild(originalRecordNode);
      failedRecord.getDocumentElement().appendChild(originalElement);
    }
  }
  /**
   * Adds error log statements as XML elements to failed record XML
   *
   * @param failedRecord
   */
  private void addErrors(Document failedRecord) {
    Element errorsElement = failedRecord.createElement("record-errors");
    for (RecordError error : errors) {
      Element errorElement = failedRecord.createElement("error");
      Element labelElement = failedRecord.createElement("label");
      labelElement.setTextContent(error.getLabel());
      Element errorTypeElement = failedRecord.createElement("type");
      errorTypeElement.setTextContent(error.getType());
      Element messageElement = failedRecord.createElement("message");
      messageElement.setTextContent(error.getBriefMessage());
      Element storageEntityElement = failedRecord.createElement("storage-entity");
      storageEntityElement.setTextContent(error.getStorageEntity());
      errorElement.appendChild(labelElement);
      errorElement.appendChild(errorTypeElement);
      errorElement.appendChild(messageElement);
      errorElement.appendChild(storageEntityElement);
      errorsElement.appendChild(errorElement);
    }
    failedRecord.getDocumentElement().appendChild(errorsElement);
  }


}
