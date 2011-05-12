package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.dom.DOMResult;

public class DOMResultVisitor implements StepVisitor {
	
	DOMResult result;
	
	public DOMResultVisitor(DOMResult result) {
		this.result = result;
	}
	
	@Override
	public StepVisitor visit(MessageProducer step) throws IOException {
		return null;
	}

	@Override
	public StepVisitor visit(MessageConsumer step) throws IOException {
		step.consume(this);
		return null;
	}

	@Override
	public StepVisitor visit(TransformationStep step) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

}
