package com.indexdata.masterkey.localindices.step;

import java.io.IOException;

public interface MessageConsumer extends Step {
	
	StepResult consume(StepVisitor source) throws IOException;

}