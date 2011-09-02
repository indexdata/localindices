package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class TransformationChainRecordStorageProxy extends RecordStorageProxy 
{
	private PipedOutputStream output;
	private PipedInputStream input;
	private Thread thread = null;
	private SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
	private Transformer transformer;
	private TransformerException transformException = null;

	public TransformationChainRecordStorageProxy(final RecordStorage storage,
			final XMLReader xmlFilter, final ContentHandler storageHandler) throws IOException,
			TransformerConfigurationException {
		//this.xmlFilter = xmlFilter;
		setTarget(storage);
		input = new PipedInputStream();
		output = new PipedOutputStream(input);
		transformer = stf.newTransformer();
		//this.storageHandler = storageHandler;  
		thread = new Thread(new Runnable() {
			public void run() {
				processDataFromInputStream(input);
			};

			private void processDataFromInputStream(PipedInputStream input) {
				InputSource source = new InputSource(input);
				SAXResult result = new SAXResult(storageHandler);
				SAXSource transformSource = new SAXSource(xmlFilter, source);
				try {
					transformer.transform(transformSource, result);
				} catch (TransformerException e) {
					e.printStackTrace();
					transformException = e;
				}
			};
		});
		thread.start();
	}

	@Override
	public void commit() throws IOException {
		try {
			// Close the output so the PipedInputStream will get the EOF. 
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Ensure that the PipedInputStream has read it all and the transformation has finished
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (transformException != null) {
			throw new IOException("Transformation failed", transformException);
		}
		super.commit();
	}

	public OutputStream getOutputStream() {
		return output;
	}

}
