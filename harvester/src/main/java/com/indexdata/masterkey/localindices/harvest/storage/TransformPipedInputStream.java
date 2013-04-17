package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.xml.factory.XmlFactory;

public class TransformPipedInputStream implements Runnable {

  ContentHandler storageHandler;
  InputStream input;
  OutputStream output;
  XMLReader xmlFilteredReader;
  RecordHarvestJob job;
  StorageJobLogger logger;
  static private SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory
      .newTransformerInstance();
  private Transformer transformer;

  public TransformPipedInputStream(InputStream input, OutputStream output,
      ContentHandler storageHandler, RecordHarvestJob job) throws TransformerConfigurationException {
    this.storageHandler = storageHandler;
    this.job = job;
    this.logger = job.getLogger();
    transformer = stf.newTransformer();
    this.input = input;
    this.output = output;
  }

  public void run() {
    processDataFromInputStream(input);
  };

  private void processDataFromInputStream(InputStream input) {
    InputSource source = new InputSource(input);
    SAXSource transformSource = new SAXSource(xmlFilteredReader, source);
    SAXResult result = new SAXResult(storageHandler);
    try {
      transformer.transform(transformSource, result);
    } catch (TransformerException e) {
      if (logger != null)
	logger.error("Transformation exception. Save for later ", e);
      e.printStackTrace();
      // TODO call back to Job with exception.
      // transformException = e;
      try {
	output.close();
      } catch (IOException ioe) {
	logger.error("IOException while closing output");
      }
    }
  };
};
