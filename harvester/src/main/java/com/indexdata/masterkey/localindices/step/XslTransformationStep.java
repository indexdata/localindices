package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

public class XslTransformationStep implements TransformationStep, Step {
	
	private String xslTransformation;
	private TransformerFactory factory;
	private Transformer transformer;
	Step next;
	
	public XslTransformationStep() {
		factory = TransformerFactory.newInstance();
	} 
	
	void setXsl(String xslTransformation) throws TransformerConfigurationException {
		this.xslTransformation = xslTransformation;
		transformer = factory.newTransformer(new StreamSource(new StringReader(this.xslTransformation)));  
	};
	
	public TransformResult transform(Source input, MessageConsumer output) throws IOException 
	{
		if (transformer != null) {
			try {
				DOMResult result = new DOMResult(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()); 
				transformer.transform(input, result);
				output.accept(new DOMResultVisitor(result));
			} catch (TransformerException e) {
				e.printStackTrace();
				throw new IOException("Transformation error", e);
			} catch (FactoryConfigurationError e) {
				e.printStackTrace();
				throw new IOException("FactoryConfiguration error", e);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new TransformResult() {
				/* TODO Implement */
			};
		}
		throw new IOException("Missing Transformer");		
	}

	@Override
	public StepResult doit(StepVisitor message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StepResult accept(StepVisitor visitor) {
		try {
			visitor = visitor.visit(this);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to accept", e);
		}
		return next.accept(visitor);
	}

	@Override
	public void setNext(Step step) {
		this.next = step;
	}

	@Override
	public Step getNext() {
		return next;
	}

}
