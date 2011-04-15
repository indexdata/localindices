package com.indexdata.masterkey.localindices.step;

public interface Step {
	
	StepResult accept(StepVisitor visitor);
}
