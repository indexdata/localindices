package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.CharArrayReader;
import java.lang.reflect.Constructor;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.indexdata.masterkey.localindices.entity.CustomTransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.xml.factory.XmlFactory;


public class RouterFactory {
  SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
  RecordHarvestJob job;
  StorageJobLogger logger;
  static RouterFactory instance = null; 

  public static synchronized RouterFactory newInstance(RecordHarvestJob job) {
    instance = new RouterFactory();
    instance.job = job;
    instance.logger = job.getLogger();
    return instance;
  }
  
  @SuppressWarnings("rawtypes")
  public MessageRouter create(TransformationStep step) {
    try {
      if (step instanceof XmlTransformationStep) {
	return createXmlTransformerRouter((XmlTransformationStep) step);
      }
      if (step instanceof CustomTransformationStep) {
	return createCustomTransformerRouter((CustomTransformationStep) step);
      }
      
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  private Templates getTemplates(StreamSource source)
      throws TransformerConfigurationException {
    return stf.newTemplates(source);
  }

  @SuppressWarnings("rawtypes")
  private MessageRouter createXmlTransformerRouter(XmlTransformationStep step) throws TransformerConfigurationException {
    try {
	logger.debug("Creating Transformer for step: " + step.getName());
	XmlTransformerRouter router = new XmlTransformerRouter(step, job);
	Templates templates = getTemplates(new StreamSource(new CharArrayReader(step.getScript().toCharArray())));
	Transformer transformer = templates.newTransformer();
	router.setXmlTransformer(transformer);
	return router;
    } catch (TransformerConfigurationException te) {
	logger.error("Error creating template for step: " + step.getName()
	    + ". Message: " + te.getMessage());
	throw te;
    }
  }  

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private MessageRouter createCustomTransformerRouter(CustomTransformationStep step)  {
    try {
	logger.debug("Creating CustomTransformer for step: " + step.getName() + ". Custom Class: " + step.getCustomClass());
	String className = step.getCustomClass();
	if (className != null) {
	  Class<? extends MessageRouter> messageRouterClass = (Class<? extends MessageRouter>) Class.forName(className);
	  Constructor<? extends MessageRouter> constructor =  messageRouterClass.getConstructor(new Class[] {step.getClass(), job.getClass()});
	  MessageRouter router = constructor.newInstance(step, logger);
	  return router;
	}
    } catch (Exception e) {
	logger.error(e.getMessage(), e);
	e.printStackTrace();
	throw new RuntimeException(e.getMessage(), e);
    }
    return null;
  }  

  

}
