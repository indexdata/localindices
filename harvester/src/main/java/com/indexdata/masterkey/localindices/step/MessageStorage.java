package com.indexdata.masterkey.localindices.step;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

public class MessageStorage implements MessageConsumer {

	private HarvestStorage storage; 
	public MessageStorage(HarvestStorage storage) {
		this.storage = storage;
	}
	
	
	@Override
	public StepResult accept(StepVisitor visitor) {
		try {
			visitor = visitor.visit(this);
		} catch (IOException e) {
			e.printStackTrace();
			return new StepResultError(e);
		}
		return new StepResult() {
			
		};
	}

	@Override
	public StepResult consume(StepVisitor stepVisitor) throws IOException {
		InputStream input = stepVisitor.getInputStream();
		OutputStream output = storage.getOutputStream();
		Reader reader = new InputStreamReader(input);
		Writer writer = new OutputStreamWriter(output);
		char[] buffer = new char[1024];
		while (reader.read(buffer) > 0) {
			writer.write(buffer);
		}
		return null;
	}

}
