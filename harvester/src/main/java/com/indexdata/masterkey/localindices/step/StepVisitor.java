package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;

public interface StepVisitor {
	
	StepVisitor visit(MessageProducer step) throws IOException;
	StepVisitor visit(MessageConsumer step) throws IOException;
	StepVisitor visit(TransformationStep step) throws IOException;
	
	InputStream getInputStream();

}
