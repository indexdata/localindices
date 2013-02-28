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

import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.client.XmlMarcClient;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;
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
    setLogger((new FileStorageJobLogger(getClass(), resource)));
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
      getStorage().setLogger(logger);
      // This is different from old behavior. All insert is now done in one commit.
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

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }
}
