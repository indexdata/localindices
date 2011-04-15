package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TransformationStep extends MessageProducer {

	StepResult doit(StepVisitor message);

	TransformResult transform(InputStream reader, OutputStream output) throws IOException;
}
