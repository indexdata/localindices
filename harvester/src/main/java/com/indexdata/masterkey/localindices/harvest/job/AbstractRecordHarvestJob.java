/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.ThreadedTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.xml.factory.XmlFactory;
import com.indexdata.xml.filter.MessageConsumer;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author jakub
 */
public abstract class AbstractRecordHarvestJob extends AbstractHarvestJob implements RecordHarvestJob {
  private RecordStorage storage;
  //protected TransformerFactory stf = XmlFactory.newTransformerInstance();
  protected SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  protected StorageJobLogger logger;
  protected Templates templates[];
  protected String error;
  boolean debug = false; 
  boolean useParallel =  true;
  RecordStorage  transformationStorage;
  @Override
  public final void setStorage(RecordStorage storage) {
    this.storage = storage;
  }

  @Override
  public synchronized RecordStorage getStorage() {
    if (transformationStorage == null) {
      try {
	if (useParallel )
	  transformationStorage = new ThreadedTransformationRecordStorageProxy(storage, templates,
	    logger);
	else
	  transformationStorage = new TransformationRecordStorageProxy(storage, templates,
		    logger);
	  
      } catch (TransformerConfigurationException e) {
	e.printStackTrace();
      } catch (IOException e) {
	e.printStackTrace();
      }
    }
    return transformationStorage;
  }

  @Override
  public abstract String getMessage();

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

  private void debugSource(Source xmlSource) {
    if (debug) {
        logger.debug("Transform xml ");
        StreamResult debugOut = new StreamResult(System.out);
        try {
          stf.newTransformer().transform(xmlSource, debugOut);
  
        } catch (Exception e) {
          logger.debug("Unable to print XML: " + e.getMessage());
        }
    }
  }

  private void debugSource(Node xml) {
    debugSource(new DOMSource(xml));
  }
  protected class TransformerConsumer implements MessageConsumer 
  {
    @Override
    public void accept(Node xmlNode) {
      accept(new DOMSource(xmlNode));
    }

    @Override
    public void accept(Source xmlNode) {
      try {
	convert(transformNode(xmlNode));
      } catch (TransformerException e) {
	logger.error("Failed to transform or convert xmlNode: " + e.getMessage() + " " + xmlNode.toString());
	e.printStackTrace();
      }
    }
  }

  protected Source transformNode(Source xmlSource) throws TransformerException {
    Transformer transformer;
    if (templates == null)
      return xmlSource;
    // TODO parallel with message queues? 
    for (Templates template : templates) {
      transformer = template.newTransformer();
      DOMResult result = new DOMResult();
      debugSource(xmlSource);
      transformer.transform(xmlSource, result);
      debugSource(result.getNode());
      
      if (result.getNode() == null) {
        logger.warn("transformNode: No Node found");
        xmlSource = new DOMSource();
      } else
        xmlSource = new DOMSource(result.getNode());
    }
    return xmlSource;
  }  
  
  protected Record convert(Source source) throws TransformerException {
    if (source != null) {
      // TODO Need to handle other RecordStore types.
      ContentHandler pzContentHandler = getStorage().getContentHandler();
      SAXResult outputTarget = new SAXResult(pzContentHandler);
      Transformer transformer = stf.newTransformer();
      transformer.transform(source, outputTarget);
    }    
    return null;
  }

}
