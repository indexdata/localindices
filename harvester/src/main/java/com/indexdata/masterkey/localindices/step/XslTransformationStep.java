package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
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
	
	public TransformResult transform(InputStream input, OutputStream output) throws IOException 
	{
		if (transformer != null) {
			Writer writer = new OutputStreamWriter(output); 
			Reader reader = new InputStreamReader(input);
			try {
				transformer.transform(new StreamSource(reader), new StAXResult(XMLOutputFactory
				        .newInstance().createXMLStreamWriter(writer)));
			} catch (TransformerException e) {
				e.printStackTrace();
				throw new IOException("Transformation error", e);
			} catch (XMLStreamException e) {
				e.printStackTrace();
				throw new IOException("XMLStreamException error", e);
			} catch (FactoryConfigurationError e) {
				e.printStackTrace();
				throw new IOException("FactoryConfiguration error", e);
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
