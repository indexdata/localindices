package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;

import com.indexdata.masterkey.localindices.step.Message;

public interface MessageStore {
	
	void accept(Message message) throws UnsupportedMessageException, IOException;  

}
