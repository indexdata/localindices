/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;
import com.indexdata.masterkey.localindices.harvest.storage.ThreadedTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.notification.Notification;
import com.indexdata.masterkey.localindices.notification.NotificationException;
import com.indexdata.masterkey.localindices.notification.Sender;
import com.indexdata.masterkey.localindices.notification.SenderFactory;
import com.indexdata.masterkey.localindices.notification.SimpleNotification;
import com.indexdata.xml.filter.SplitContentHandler;

/**
 * Specifies the simplest common behavior of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author Dennis
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
    // Invalidate transformation storage proxy
    this.transformationStorage = null;
    this.storage = storage;
  }
  
  @Override
  public synchronized RecordStorage getStorage() {
    if (transformationStorage == null) {
      Transformation transformation = getHarvestable().getTransformation();
      List<TransformationStep> steps = null;
      Boolean parallel = false;
      if (transformation != null) {
	steps = transformation.getSteps();
	parallel = transformation.getParallel();
      }
      try {
	if (new Boolean(true).equals(parallel))
	  transformationStorage = new ThreadedTransformationRecordStorageProxy(storage, steps, this);
	else
	  transformationStorage = new TransformationRecordStorageProxy(storage, steps, this);
	  
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

  @Override
  public StorageJobLogger getLogger() {
    return logger;
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Deprecated
  protected RecordStorage setupTransformation(RecordStorage storage) {
    Harvestable resource = getHarvestable(); 
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      boolean split = (splitSize > 0 && splitDepth > 0);
      try {
	XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
	if (split) {
	  // TODO check if the existing one exists and is alive. 
	  if (streamStorage == null || streamStorage.isClosed() == true) {
	    SplitContentHandler splitHandler = new SplitContentHandler(new RecordStorageConsumer(getStorage(),logger), splitDepth, splitSize);
	    xmlReader.setContentHandler(splitHandler);
	    streamStorage = new SplitTransformationChainRecordStorageProxy(storage, xmlReader, this);
	  }
	  return streamStorage;
	}
	return new TransformationChainRecordStorageProxy(storage, xmlReader, this);

      } catch (Exception e) {
	logger.error(e.getMessage(),e);
      }
    }
    logger.warn("No Transformation Proxy configured.");
    return storage;
  }

  public OutputStream getOutputStream() 
  {
    // Currently, the client MUST only called getOutputStream once per XML it wants to parse and split
    // So multiple XML files can be read by calling getOutputStream again, but also leave it open for a bad client 
    // to misuse this call. This could be avoided by requiring the client to call close on stream between XML files, 
    // intercept the close call and null transformationStorage, but reuse otherwise.
    // Though, each thread needs it's own 

    return setupTransformation(getStorage()).getOutputStream();
  }

  protected void commit() throws IOException {
    RecordStorage storage = getStorage();
    storage.commit();
    Harvestable resource = getHarvestable();
    resource.setLastHarvestFinished(new Date());
    try {
      StorageStatus storageStatus = storage.getStatus();  
      if (storageStatus != null) {
        resource.setAmountHarvested(storageStatus.getAdds());
        logger.info("Committed "  
            	     + storageStatus.getAdds() + " adds, "  
            	     + storageStatus.getDeletes() + " deletes. " 
            	     + storageStatus.getTotalRecords() + " in total (pending warming of index).");
      }
    }
    catch (StatusNotImplemented exception) {
      logger.warn("Failed to get Storage Status.");
    }
    markForUpdate();
  }

  protected void mailMessage(String subject, String message) {
    Sender sender = SenderFactory.getSender();
    String status = getStatus().toString();
    Harvestable harvestable = getHarvestable();
    if (harvestable.getMailLevel() != null && checkMailLevel(HarvestStatus.valueOf(harvestable.getMailLevel()), getStatus())) {
      Notification msg = new SimpleNotification(status, 
  		harvestable.getName() + "(" + harvestable.getId() + "): "  + subject, message);
      try {
        if (sender != null) {
          String customRecievers = harvestable.getMailAddress();
          if (customRecievers != null && !"".equals(customRecievers))
            sender.send(customRecievers, msg);
          else
            sender.send(msg);
        } else {
          throw new NotificationException("No Sender configured", null);
        }
      } catch (NotificationException e1) {
        logger.error("Failed to send notification " + e1.getMessage());
      }
    }
  }

  private boolean checkMailLevel(HarvestStatus mailLevel, HarvestStatus status) {
    if (mailLevel.ordinal() <= status.ordinal())
      return true;
    return false;
  }

  protected void logError(String logSubject, String message) {
    setStatus(HarvestStatus.ERROR, message);
    getHarvestable().setMessage(message);
    logger.error(logSubject + ": " +  message);
  }
  
  protected void shutdown() {
    getHarvestable().setDiskRun(false);
    markForUpdate();
    try {
	getStorage().shutdown();
    } catch (IOException ioe) {
	logger.warn("Storage shutdown exception: " + ioe.getMessage());
    } finally {
      transformationStorage = null;
      logger.close();
    }
  }
}
