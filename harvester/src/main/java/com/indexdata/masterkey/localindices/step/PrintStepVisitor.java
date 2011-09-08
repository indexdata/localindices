package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PrintStepVisitor implements StepVisitor {

	private InputStream input;
	private OutputStream output; 
	
	@Override
	public StepVisitor visit(MessageProducer step) {
		System.out.println("MessageProducer");
		if (step.getNext() != null)
			step.getNext().accept(this);
		return this;
	}

	@Override
	public StepVisitor visit(MessageConsumer step) {
		System.out.println("MessageConsumer");
		try {
			// TODO why VisitorStep? 
			step.consume(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public StepVisitor visit(TransformationStep step) throws IOException 
	{
		System.out.println("Transform Step");
		//new StreamResult(output);
		// TransformResult result = step.transform(new StreamSource(this.getInputStream()), null);
		// TODO react on the result
		return this;
	}

	public InputStream getInputStream() {
		return input;
	}

	public OutputStream getOutputStream() {
		return output;
	}

}
