package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class TransformationChainStorageProxy extends StorageProxy {

	PipedOutputStream output;
	PipedInputStream input;
	XMLReader xmlFilter;
	Thread thread = null;
	SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory
			.newInstance();
	Transformer transformer;
	TransformerException transformException = null;
	IOException rollbackException = null;

	public TransformationChainStorageProxy(final HarvestStorage storage,
			final XMLReader xmlFilter) throws IOException,
			TransformerConfigurationException {
		this.xmlFilter = xmlFilter;
		setTarget(storage);
		input = new PipedInputStream();
		output = new PipedOutputStream(input);
		transformer = stf.newTransformer();
		thread = new Thread(new Runnable() {
			public void run() {
				processDataFromInputStream(input);
			};

			private void processDataFromInputStream(PipedInputStream input) {
				InputSource source = new InputSource(input);
				StreamResult result = new StreamResult(getTarget()
						.getOutputStream());
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
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (transformException != null) {
			throw new IOException("Transformation failed", transformException);
		}
	}

	public OutputStream getOutputStream() {
		return output;
	}

}
