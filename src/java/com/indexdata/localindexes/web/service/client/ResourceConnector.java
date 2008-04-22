/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.web.service.client;

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
 * @param T data-binding container type
 */
public class ResourceConnector<T> {

    private URL url;
    private String mimeType = "application/xml";
    private String entityPackages;
    private Class entityType;
    private JAXBContext jaxbCtx;

    public ResourceConnector(URL url, Class<T> type) {
        this.url = url;
        this.entityType = type;
    }

    public ResourceConnector(URL url, String entityPackages) {
        this.url = url;
        this.entityPackages = entityPackages;
    }

    private JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null) {
            if (entityPackages != null) {
                jaxbCtx = JAXBContext.newInstance(entityPackages);
            } else {
                jaxbCtx = JAXBContext.newInstance(entityType);
            }
        }
        return jaxbCtx;
    }

    public T get() throws Exception {
        Object obj = null;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            JAXBContext context = getJAXBContext();
            obj = context.createUnmarshaller().unmarshal(conn.getInputStream());
        } else {
            throw new Exception("Cannot retrieve resource");
        }

        return (T) obj;
    }

    public void put(T t) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");

        JAXBContext context = getJAXBContext();
        context.createMarshaller().marshal(t, conn.getOutputStream());

        int responseCode = conn.getResponseCode();
        switch (responseCode) {
            case 200:   //OK
            case 201:   //Created
            case 202:   //Accpeted
            case 203:   //Non-authoritative
            case 204:   //No-content
            case 205:   //Reset
            case 206:   //Partial
                break;
            case 405:
                throw new Exception("Cannot update resource (HTTP method not allowed).");
            default:
                throw new Exception("Cannot update resource (server returned " + responseCode + ")");
        }
    }

    public void delete() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");

        int responseCode = conn.getResponseCode();
        switch (responseCode) {
            case 200:   //OK
            case 201:   //Created
            case 202:   //Accpeted
            case 203:   //Non-authoritative
            case 204:   //No-content
            case 205:   //Reset
            case 206:   //Partial
                break;
            case 405:
                throw new Exception("Cannot delete resource (HTTP method not allowed).");
            default:
                throw new Exception("Cannot delete resource (server returned " + responseCode + ")");
        }
    }

    public void post(T t) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        JAXBContext context = getJAXBContext();
        context.createMarshaller().marshal(t, conn.getOutputStream());

        int responseCode = conn.getResponseCode();
        switch (responseCode) {
            case 200:   //OK
            case 201:   //Created
            case 202:   //Accpeted
            case 203:   //Non-authoritative
            case 204:   //No-content
            case 205:   //Reset
            case 206:   //Partial
                break;
            case 405:
                throw new Exception("Cannot create resource (HTTP method not allowed).");
            default:
                throw new Exception("Cannot create resource (server returned " + responseCode + ")");
        }
    }

    public void postAny(Object obj) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", mimeType);
        conn.setRequestMethod("POST");

        JAXBContext context = getJAXBContext();
        context.createMarshaller().marshal(obj, conn.getOutputStream());

        int responseCode = conn.getResponseCode();
        switch (responseCode) {
            case 200:   //OK
            case 201:   //Created
            case 202:   //Accpeted
            case 203:   //Non-authoritative
            case 204:   //No-content
            case 205:   //Reset
            case 206:   //Partial
                break;
            case 405:
                throw new Exception("Cannot create resource (HTTP method not allowed).");
            default:
                throw new Exception("Cannot create resource (server returned " + responseCode + ")");
        }
    }
}
