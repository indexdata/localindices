/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.CharArrayReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.xml.factory.XmlFactory;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author jakub
 */
public abstract class AbstractRecordHarvestJob implements RecordHarvestJob {
  private boolean updated;
  private RecordStorage storage;
  private HarvestStatus status;
  private boolean die;
  //protected TransformerFactory stf = XmlFactory.newTransformerInstance();
  protected SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  protected StorageJobLogger logger;
  protected Templates templates[];
  protected String error; 

  protected final void setStatus(HarvestStatus status) {
    this.status = status;
  }

  protected final void markForUpdate() {
    updated = true;
  }

  @Override
  public synchronized boolean isKillSent() {
    return die;
  }

  @Override
  public final synchronized void kill() {
    if (status == HarvestStatus.RUNNING) {
      status = HarvestStatus.KILLED;
    }
    die = true;
  }

  @Override
  public final HarvestStatus getStatus() {
    return status;
  }

  @Override
  public final void setStorage(RecordStorage storage) {
    this.storage = storage;
  }

  @Override
  public final RecordStorage getStorage() {
    return this.storage;
  }

  @Override
  public final synchronized void finishReceived() {
    if (status != null && status.equals(HarvestStatus.FINISHED)) {
      status = HarvestStatus.OK;
    }
  }

  @Override
  public abstract String getMessage();

  @Override
  public final boolean isUpdated() {
    return updated;
  }

  @Override
  public final void clearUpdated() {
    updated = false;
  }

  @Override
  public abstract void run();

  @SuppressWarnings("unused")
  private Templates[] getTemplates(String[] stringTemplates)
      throws TransformerConfigurationException {
    StreamSource[] streamSources = new StreamSource[stringTemplates.length];
    int index = 0;
    for (String template : stringTemplates) {
      streamSources[index] = new StreamSource(new CharArrayReader(
	  template.toCharArray()));
      index++;
    }
    return getTemplates(streamSources);
  }

  private Templates getTemplates(StreamSource source)
      throws TransformerConfigurationException {
    return stf.newTemplates(source);
  }

  protected Templates[] getTemplates(StreamSource[] sourceTemplates)
      throws TransformerConfigurationException {

    Templates[] templates = new Templates[sourceTemplates.length];
    int index = 0;
    for (StreamSource source : sourceTemplates) {
      templates[index] = stf.newTemplates(source);
      index++;
    }
    return templates;
  }

  protected Templates[] lookupTransformationTemplates(
      Transformation transformation) throws TransformerConfigurationException {
    if (transformation.getSteps() == null)
      return new Templates[0];

    List<TransformationStep> steps = transformation.getSteps();
    Templates[] templates = new Templates[steps.size()];
    for (int index = 0; index < steps.size(); index++) {
      TransformationStep step = steps.get(index);
      try {
	logger.debug("Creating template for step: " + step.getName());
	templates[index] = getTemplates(new StreamSource(new CharArrayReader(
	    step.getScript().toCharArray())));
      } catch (TransformerConfigurationException te) {
	logger.error("Error creating template for step: " + step.getName()
	    + ". Message: " + te.getMessage());
	throw te;
      }
    }
    return templates;
  }

  public StorageJobLogger getLogger() {
    return logger;
  }

  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  public XMLReader createTransformChain(boolean split) throws ParserConfigurationException,
      SAXException, TransformerConfigurationException, UnsupportedEncodingException {
        // Set up to read the input file
        SAXParserFactory spf = XmlFactory.newSAXParserFactoryInstance();
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        // If split mode, we are just interested in a reader. The transformation is done in transformNode();
        if (split)
          return reader;
        XMLFilter filter;
        XMLReader parent = reader;
        int index = 0;
        while (templates != null && index < templates.length) {
          filter = stf.newXMLFilter(templates[index]);
          filter.setParent(parent);
          parent = filter;
          index++;
        }
        return parent;
      }

  protected void setupTemplates(Harvestable resource, List<TransformationStep> steps) {
    templates = new Templates[steps.size()];
    int index = 0;
    String stepInfo = "";
    String stepScript =""; 
    try {
      for (TransformationStep step : steps) {
        stepInfo =  step.getId() + " " + step.getName();
        if (step.getScript() != null) {
          stepScript = step.getScript();
          logger.info("Setting up XSLT template for Step: " + stepInfo);
          templates[index] = stf.newTemplates(new StreamSource(new StringReader(step.getScript())));
          index++;
        }
        else {
          logger.warn("Step " + stepInfo + " has not script!");
        }
      }
    } catch (TransformerConfigurationException tce) {
      error = "Failed to build xslt templates: " + stepInfo;
      templates = new Templates[0];
      logger.error("Failed to build XSLT template for Step: " + stepInfo + "Script: " + stepScript);      
      logger.error(error);
      setStatus(HarvestStatus.ERROR);
    }
  }  
}
