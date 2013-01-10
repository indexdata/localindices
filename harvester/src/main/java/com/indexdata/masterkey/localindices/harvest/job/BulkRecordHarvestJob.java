/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.client.XmlMarcClient;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;
import com.indexdata.xml.filter.MessageConsumer;
import com.indexdata.xml.filter.SplitContentHandler;

/**
 * This class handles HTTP download of file(s), and bulk transformation
 * 
 * @author Dennis Schafroth
 * 
 */
public class BulkRecordHarvestJob extends AbstractRecordHarvestJob 
{
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private XmlBulkResource resource;
  private RecordStorage transformationStorage;
  private Proxy proxy;
  private int splitSize = 0;
  private int splitDepth = 0;

  public BulkRecordHarvestJob(XmlBulkResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    splitDepth = getNumber(resource.getSplitAt(), splitDepth); 
    splitSize  = getNumber(resource.getSplitSize(), splitSize);
    this.resource.setMessage(null);
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    setLogger((new StorageJobLogger(getClass(), resource)));
    List<TransformationStep> steps = resource.getTransformation().getSteps();
    setupTemplates(resource, steps);
  }

  private int getNumber(String value, int defaultValue) {
    int number;
    if (value != null && !"".equals(value)) {
      try {
	number = Integer.parseInt(value);
	if (number < 0)
	  number = defaultValue;
	return number;
      } catch (NumberFormatException nfe) {
	logger.warn("Unable to parse number: " + value);
      }
    }
    return defaultValue;
  }

  private Record convert(Source source) throws TransformerException {
    if (source != null) {
      // TODO Need to handle other RecordStore types.
      SAXResult outputTarget = new SAXResult(new Pz2SolrRecordContentHandler(getStorage(), resource
	  .getId().toString()));
      Transformer transformer = stf.newTransformer();
      transformer.transform(source, outputTarget);
    }
    return null;
  }
  
  private void debugSource(Source xmlSource) {
    boolean debug = false;
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

  protected Source transformNode(Source xmlSource) throws TransformerException {
    Transformer transformer;
    if (templates == null)
      return xmlSource;
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

  class TransformerConsumer implements MessageConsumer 
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

  public RecordStorage setupTransformation(RecordStorage storage) {
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      boolean split = (splitSize > 0 && splitDepth > 0);
      XMLReader xmlReader;
      try {
	xmlReader = createTransformChain(split);
	if (split) {
	  SplitContentHandler splitHandler = new SplitContentHandler(new TransformerConsumer(), splitDepth, splitSize);
	  xmlReader.setContentHandler(splitHandler);
	  return new SplitTransformationChainRecordStorageProxy(storage, xmlReader, logger);
	}
	return new TransformationChainRecordStorageProxy(storage, xmlReader,
	    new Pz2SolrRecordContentHandler(storage, resource.getId().toString()), logger);

      } catch (Exception e) {
	e.printStackTrace();
	logger.error(e.getMessage());
      }
    }
    logger.warn("No Transformation Proxy configured.");
    return storage;
  }

  public OutputStream getOutputStream() 
  {
    transformationStorage = setupTransformation(getStorage());
    return transformationStorage.getOutputStream();
  }
  
  public String getMessage() {
    return error;
  }

  public void run() {
    try {
      // Don't start if we already are in error
      if (getStatus() == HarvestStatus.ERROR) {
	throw new Exception(error);
      }
      // This is different from old behavior. All insert is now done in one commit.
      getStorage().setLogger(logger);
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
	getStorage().purge(false);
      setStatus(HarvestStatus.RUNNING);
      downloadList(resource.getUrl().split(" "));
      setStatus(HarvestStatus.FINISHED);
      // A bit weird, that we need to close the transformation, but in order to flush out all records in the pipeline
      transformationStorage.databaseEnd();
      transformationStorage.commit();
      //getStorage().commit();
    } catch (Exception e) {
      // Test
      e.printStackTrace();      
      try {
	getStorage().rollback();
      } catch (Exception ioe) {
	logger.warn("Roll-back failed.", ioe);
      }
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.error("Download failed.", e);
    } finally {
      logger.close();
    }
  }

  private void downloadList(String[] urls) throws Exception 
  {
    XmlMarcClient client = new XmlMarcClient();
    client.setHarvestJob(this);
    client.setProxy(proxy);
    client.setLogger(logger);
    client.setHarvestable(resource);
    for (String url : urls) {
      client.download(new URL(url));
    }
  }

  @Override
  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) {
      super.setStorage((RecordStorage) storage);
    }
    else {
      setStatus(HarvestStatus.ERROR);
      resource.setCurrentStatus("Unsupported StorageType: " + storage.getClass().getCanonicalName()
	  + ". Requires RecordStorage");
    }
  }
}
