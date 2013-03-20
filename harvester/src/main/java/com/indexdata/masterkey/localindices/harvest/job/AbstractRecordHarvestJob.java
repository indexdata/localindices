/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.ThreadedTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.xml.filter.SplitContentHandler;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author jakub
 */
public abstract class AbstractRecordHarvestJob extends AbstractHarvestJob implements RecordHarvestJob {
  private RecordStorage storage;
  protected StorageJobLogger logger;
  protected String error;
  boolean debug = false; 
  boolean useParallel =  false;
  SplitTransformationChainRecordStorageProxy  streamStorage;
  RecordStorage  transformationStorage;
  protected int splitSize = 1;
  protected int splitDepth = 1;

  
  @Override
  public void setStorage(RecordStorage storage) {
    this.storage = storage;
  }
  
  @Override
  public synchronized RecordStorage getStorage() {
    if (transformationStorage == null) {
      Transformation transformation = getHarvestable().getTransformation();
      List<TransformationStep> steps = null;
      if (transformation != null) {
	steps = transformation.getSteps();
      }
      try {
	if (new Boolean(false).equals(transformation.getParallel()))
	  transformationStorage = new TransformationRecordStorageProxy(storage, steps, this);
	else
	  transformationStorage = new ThreadedTransformationRecordStorageProxy(storage, steps, this);
	  
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

  public StorageJobLogger getLogger() {
    return logger;
  }

  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  protected RecordStorage setupTransformation(RecordStorage storage) {
    Harvestable resource = getHarvestable(); 
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      boolean split = (splitSize > 0 && splitDepth > 0);
      try {
	XMLReader xmlReader = null;
	xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
	if (split) {
	  // TODO check if the existing one exists and is alive. 
	  if (streamStorage == null || streamStorage.isClosed() == true) {
	    SplitContentHandler splitHandler = new SplitContentHandler(new RecordStorageConsumer(getStorage()), splitDepth, splitSize);
	    xmlReader.setContentHandler(splitHandler);
	    streamStorage = new SplitTransformationChainRecordStorageProxy(storage, xmlReader, this);
	  }
	  return streamStorage;
	}
	return new TransformationChainRecordStorageProxy(storage, xmlReader, this);

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
    // TODO build in logic only to return new Split proxy when output stream has been close.
    
    // Currently, the client MUST only called getOutputStream once per XML it wants to parse and split
    // So multiple XML files can be read by calling getOutputStream again, but also leave it open for a bad client 
    // to misuse this call. This could be avoided by requiring the client to call close on stream between XML files, 
    // intercept the close call and null transformationStorage, but reuse otherwise.

    // Though, each thread needs it's own 

    return setupTransformation(getStorage()).getOutputStream();
  }
}
