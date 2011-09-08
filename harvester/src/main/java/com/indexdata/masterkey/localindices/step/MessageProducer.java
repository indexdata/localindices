package com.indexdata.masterkey.localindices.step;

public interface MessageProducer extends Step {

	void setNext(Step step);
	Step getNext();
}
