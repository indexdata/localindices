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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.IntPredicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;

import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.cliftonlabs.json_simple.Jsoner;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.utils.XmlUtils;

/**
 * Holds errors encountered in the processing of one Inventory record set (the
 * Inventory records derived from one incoming bibliographic record)
 *
 * @author ne
 */
public class RecordWithErrors {

  List<RecordError> errors = new ArrayList<RecordError>();
  FailedRecordsController failCtrl;
  TransformedRecord transformedRecord;
  StorageJobLogger logger;

  RecordWithErrors(TransformedRecord transformedRecord, FailedRecordsController controller) {
    this.failCtrl = controller;
    this.logger = controller.logger;
    this.transformedRecord = transformedRecord;
  }

  String getRecordIdentifier () {
    return ((String) transformedRecord.getLocalIdentifier());
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
    reportError(error, logLevel);
    if (exception != null) {
      throw new InventoryUpdateException(error.toString(),exception);
    } else {
      throw new InventoryUpdateException(error.toString());
    }
  }

  void reportError (RecordError error, Level logLevel) {
    addError(error);
    int count = failCtrl.incrementErrorCount(error);
    if (count <= 10) {
      logger.error(error.getMessageWithContext());
    } else if (count>10 && count < 100) {
      logger.log(logLevel, error.getServerMessage());
    } else if (count % 100 == 0) {
      logger.error(String.format("%d records failed with %s", failCtrl.getErrorsByShortErrorMessage(error.getShortMessageForCounting()), error.getShortMessageForCounting()));
    }
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
   */
  void writeErrorsLog(StorageJobLogger logger) {
    if (hasErrors()) {
      int i=0;
      for (RecordError error : errors) {
        i++;
        int occurrences = failCtrl.getErrorsByShortErrorMessage(error.getShortMessageForCounting());
        if (occurrences < 10) {
          if (i==1) logger.error("Error" + (numberOfErrors() > 1 ? "s" : "") + " updating Inventory with  " + transformedRecord.getJson());
          logger.error("#" + i + " " + error.getMessageWithContext());
        } else if (occurrences % 100 == 0) {
          logger.error(occurrences + " records have failed with " + error.getShortMessageForCounting());
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
    return transformedRecord.getOriginalContent();
    /*
    if (transformedRecord.getOriginalXml() != null) {
      return transformedRecord.getOriginalXml().getBytes();
    } else {
      if (transformedRecord.getOriginalContent() != null) {
        return transformedRecord.getOriginalContent();
      } else {
        logger.warn("No original record found, neither stored in Record with 'store original', nor passed through as element 'original' in transformed record XML/JSON");
        return null;
      }
    }
    */
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
    JSONObject transformedRecordExclusiveOriginal = transformedRecord.getTransformedRecordExclusiveOriginal();
    transformedElement.setTextContent(Jsoner.prettyPrint(transformedRecordExclusiveOriginal.toJSONString()));
    failedRecord.getDocumentElement().appendChild(transformedElement);
  }

  /**
   * Adds original record as either XML or a string to element `original` of the failed record XML
   * @param builder The builder for creating the XML element
   * @param failedRecord  The XML document to add the original record to
   */
  private void addOriginal(DocumentBuilder builder, Document failedRecord) {
    String originalRecordAsString = getOriginalRecord() != null ? new String(getOriginalRecord()) : "";
    Element originalElement = failedRecord.createElement("original");
    try {
      if (!originalRecordAsString.isEmpty()) {
        Document originalRecordAsDocument = builder.parse(new InputSource(new StringReader(originalRecordAsString)));
        Node originalRecordNode = failedRecord.importNode(originalRecordAsDocument.getDocumentElement(),true);
        originalElement.appendChild(originalRecordNode);
      }
    } catch (SAXException | IOException e) {
      originalElement.setTextContent( stripChars( originalRecordAsString, c -> c > '\u001F' && c != '\u007F'));
    } catch (ClassCastException cce) {
      logger.error("ClassCastException when attempting get and add original record til failed record XML; " + cce.getMessage());
    }
    failedRecord.getDocumentElement().appendChild(originalElement);
  }

  /**
   * Strip string down to select characters.
   * Purpose: strip record and field separators from MARC string before putting it in XML element
   * @param s The string to strip from
   * @param include The criteria for characters to keep
   * @return the input string, now only including select characters
   */
  static String stripChars(String s, IntPredicate include) {
    return s.codePoints().filter(include::test).collect(StringBuilder::new,
            StringBuilder::appendCodePoint, StringBuilder::append).toString();
  }

  /**
   * Adds error log statements as XML elements to failed record XML
   *
   * @param failedRecord The record to add log statements to
   */
  private void addErrors(Document failedRecord) {
    try {
      Element errorsElement = failedRecord.createElement("record-errors");
      for (RecordError error : errors) {
        Element errorElement = failedRecord.createElement("error");
        Element labelElement = failedRecord.createElement("label");
        labelElement.setTextContent(error.getAdditionalContext());
        Element typeOfErrorElement = failedRecord.createElement("type-of-error");
        typeOfErrorElement.setTextContent(error.getErrorType());
        Element messageElement = failedRecord.createElement("message");
        messageElement.setTextContent(error.getServerMessage());
        Element typeOfRecordElement = failedRecord.createElement("type-of-record");
        typeOfRecordElement.setTextContent(error.getRecordType());
        Element transactionElement = failedRecord.createElement("transaction");
        transactionElement.setTextContent(error.getTransaction());
        Element entityElement = failedRecord.createElement("entity");
        entityElement.setTextContent(error.getEntity());
        errorElement.appendChild(labelElement);
        errorElement.appendChild(typeOfErrorElement);
        errorElement.appendChild(typeOfRecordElement);
        errorElement.appendChild(transactionElement);
        errorElement.appendChild(messageElement);
        errorElement.appendChild(entityElement);
        errorsElement.appendChild(errorElement);
      }
      failedRecord.getDocumentElement().appendChild(errorsElement);
    } catch (ClassCastException cce) {
      logger.error("ClassCastException when attempting to add error messages to failed record XML; " + cce.getMessage());
    }
  }
}
