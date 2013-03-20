package com.indexdata.masterkey.localindices.harvest.messaging;

import java.io.CharArrayReader;
import java.lang.reflect.Constructor;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import com.indexdata.masterkey.localindices.entity.CustomTransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class RouterFactory {
  StorageJobLogger logger;
  static RouterFactory instance = null; 

  public static synchronized RouterFactory newInstance() {
    if (instance == null)
      instance = new RouterFactory();
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
    
    // return sft.newTemplates(source);
    return null;
  }

  @SuppressWarnings("rawtypes")
  private MessageRouter createXmlTransformerRouter(XmlTransformationStep step) throws TransformerConfigurationException {
    try {
	logger.debug("Creating Transformer for step: " + step.getName());
	XmlTransformerRouter router = new XmlTransformerRouter(step);
	Templates templates = getTemplates(new StreamSource(new CharArrayReader(
	    step.getScript().toCharArray())));
	Transformer transformer = templates.newTransformer();
	router.setXmlTransformer(transformer);
	
    } catch (TransformerConfigurationException te) {
	logger.error("Error creating template for step: " + step.getName()
	    + ". Message: " + te.getMessage());
	throw te;
    }
    return null;
  }  

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private MessageRouter createCustomTransformerRouter(CustomTransformationStep step)  {
    try {
	logger.debug("Creating CustomTransformer for step: " + step.getName() + ". Custom Class: " + step.getCustomClass());
	if (step.getCustomClass() != null) {
	  String className = step.getCustomClass();
	  Class<? extends MessageRouter> messageRouterClass = (Class<? extends MessageRouter>) Class.forName(className);
	  Constructor<? extends MessageRouter> constructor =  messageRouterClass.getConstructor(new Class[] {step.getClass()});
	  MessageRouter router = constructor.newInstance(step);
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
