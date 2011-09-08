package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SplitTransformationChainRecordStorageProxy extends RecordStorageProxy {
  private PipedOutputStream output;
  private PipedInputStream input;
  private Thread thread = null;
  private TransformerException transformException = null;

  public SplitTransformationChainRecordStorageProxy(final RecordStorage storage, final XMLReader xmlFilter) 
  	throws IOException, TransformerConfigurationException {
    setTarget(storage);
    input = new PipedInputStream();
    output = new PipedOutputStream(input);
    thread = new Thread(new Runnable() {
      public void run() {
	processDataFromInputStream(input);
      };

      private void processDataFromInputStream(PipedInputStream input) {
	try {
	  InputSource source = new InputSource(input);
	  xmlFilter.parse(source);
	} catch (IOException ioe) {
	  transformException = new TransformerException("IO Error while parsing/transforming: "
	      + ioe.getMessage(), ioe);
	  ioe.printStackTrace();
	} catch (SAXException e) {
	  transformException = new TransformerException("SAX Exception: " + e.getMessage(), e);
	  e.printStackTrace();
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
    // Ensure that the PipedInputStream has read it all and the transformation
    // has finished
    try {
      thread.join();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (transformException != null) {
      throw new IOException("Transformation failed: " + transformException.getMessage(), transformException);
    }
    super.commit();
  }

  public OutputStream getOutputStream() {
    return output;
  }

}
