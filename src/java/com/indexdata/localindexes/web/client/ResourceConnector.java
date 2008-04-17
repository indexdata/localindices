/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.client;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 *
 * @author jakub
 */
public class ResourceConnector<T> {
    private URI uri;
    private String entityPackages;
    private Class entityType;
    private static JAXBContext jaxbCtx;
    
    public ResourceConnector(URI uri, Class<T> type) {
        this.uri = uri;
        this.entityType = type;
    }
    
    public ResourceConnector(URI uri, String entityPackages) {
        this.uri = uri;
        this.entityPackages = entityPackages;
    }
    
    private JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null) {
            if (entityPackages != null)
                jaxbCtx = JAXBContext.newInstance(entityPackages);
            else
                jaxbCtx = JAXBContext.newInstance(entityType);
        }
        return jaxbCtx;
    }
    
    
    public T get() throws Exception {
        Object obj = null;
        try {
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                JAXBContext context = getJAXBContext();
                obj = context.createUnmarshaller().unmarshal(conn.getInputStream());
            } else {
                throw new Exception("Cannot retrieve resource");
            }
        } catch (Exception ex) {
            throw new Exception("Cannot retrieve resource", ex);
        }
        return (T) obj;
    }
    
    
    public void put(T t) {
        
    }
    public void delete() {
        
    }
    public void post(T t) {
        
    }

}
