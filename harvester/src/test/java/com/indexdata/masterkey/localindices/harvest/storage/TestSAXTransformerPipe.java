package com.indexdata.masterkey.localindices.harvest.storage;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.indexdata.xml.factory.XmlFactory;

import junit.framework.TestCase;

public class TestSAXTransformerPipe extends TestCase {

  public void testDummy() {

  }

  @SuppressWarnings("unused")
  private void notestPipedTransformer() throws TransformerConfigurationException, SAXException {
    TransformerFactory tFactory = XmlFactory.newTransformerInstance();

    if (tFactory.getFeature(SAXSource.FEATURE) && tFactory.getFeature(SAXResult.FEATURE)) {
      // Cast the TransformerFactory to SAXTransformerFactory.
      SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);
      // Create a TransformerHandler for each stylesheet.
      TransformerHandler tHandler1;
      tHandler1 = saxTFactory.newTransformerHandler(new StreamSource("foo1.xsl"));
      TransformerHandler tHandler2 = saxTFactory
	  .newTransformerHandler(new StreamSource("foo2.xsl"));
      TransformerHandler tHandler3 = saxTFactory
	  .newTransformerHandler(new StreamSource("foo3.xsl"));

      // Create an XMLReader.
      XMLReader reader;
      reader = XMLReaderFactory.createXMLReader();
      reader.setContentHandler(tHandler1);
      reader.setProperty("http://xml.org/sax/properties/lexical-handler", tHandler1);

      tHandler1.setResult(new SAXResult(tHandler2));
      tHandler2.setResult(new SAXResult(tHandler3));

      // transformer3 outputs SAX events to the serializer.
      /*
       * Serializer serializer =
       * SerializerFactory.getSerializer(OutputProperties
       * .getDefaultMethodProperties( "xml"));
       * serializer.setOutputStream(System.out); tHandler3.setResult(new
       * SAXResult(serializer.asContentHandler()));
       * 
       * // Parse the XML input document. The input ContentHandler and output
       * ContentHandler // work in separate threads to optimize performance.
       * reader.parse("foo.xml"); }
       */

    }
  }
}
