package com.indexdata.masterkey.localindices.step;

import java.io.IOException;

import javax.xml.transform.Source;

public interface TransformationStep extends MessageProducer {

	StepResult doit(StepVisitor message);

	TransformResult transform(Source reader, MessageConsumer sink) throws IOException;
}
