package com.indexdata.localindexes.web.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Entity provider for encoding converted entities to/from XML
 *
 * @author jakub@indexdata.com
 */
@ProduceMime("application/xml")
@Provider
public class XMLBindingProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
    
    //each-package has to contain a jaxb.index file with classes specified
    private static String entityPackage = 
            "com.indexdata.localindexes.web.entitybeans" +
            ":com.indexdata.localindexes.web.converter";
    
    private static JAXBContext jaxbCtx;
    // context creation is expensive, and since our classes don't change we can cache it
    private static JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null)
                jaxbCtx = JAXBContext.newInstance(entityPackage);
        return jaxbCtx;
    }

    public boolean isWriteable(Class<?> type) {
        return isInAnyPackage(type);
    }

    public long getSize(Object obj) {
        return -1;
    }

    public void writeTo(Object obj, MediaType mt,
            MultivaluedMap<String, Object> headers, OutputStream os)
            throws IOException {
        try {
            Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(obj, os);
        } catch (JAXBException ex) {
            throw new IOException("Error marshalling entity.", ex);
        }
    }

    public boolean isReadable(Class<?> type) {
        return isInAnyPackage(type);
    }

    public Object readFrom(Class<Object> type, MediaType mt,
            MultivaluedMap<String, String> headers, InputStream is)
            throws IOException {
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            return unmarshaller.unmarshal(is);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IOException("Error unmarshalling input message body.", ex);
        }
    }
    
    private boolean isInAnyPackage(Class<?> type) {
        for (String regPack : entityPackage.split(":")) {
            if (regPack.equals(type.getPackage().getName()))
                return true;
        }
        return false;
    }
}
