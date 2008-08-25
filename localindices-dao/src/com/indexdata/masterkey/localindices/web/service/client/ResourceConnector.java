/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.client;

import java.io.IOException;
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

    public T get() throws ResourceConnectionException {        
        Object obj = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JAXBContext context = getJAXBContext();
                obj = context.createUnmarshaller().unmarshal(conn.getInputStream());
            } else {
                throw new ResourceConnectionException("Cannot retrieve resource - status code " + responseCode);
            }
        } catch (IOException ioe) {
            throw new ResourceConnectionException(ioe);
        } catch (JAXBException jaxbe) {
            throw new ResourceConnectionException(jaxbe);
        }

        return (T) obj;
    }

    public void put(T t) throws ResourceConnectionException {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");

            JAXBContext context = getJAXBContext();
            context.createMarshaller().marshal(t, conn.getOutputStream());

            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case 200: //OK
                case 201: //Created
                case 202: //Accpeted
                case 203: //Non-authoritative
                case 204: //No-content
                case 205: //Reset
                case 206:
                    //Partial
                    break;
                case 405:
                    throw new ResourceConnectionException("Cannot update resource - HTTP method not allowed (405)");
                default:
                    throw new ResourceConnectionException("Cannot update resource - status code " + responseCode);
            }
        } catch (JAXBException jaxbe) {
            throw new ResourceConnectionException(jaxbe);
        } catch (IOException ioe) {
            throw new ResourceConnectionException(ioe);
        }
    }

    public void delete() throws ResourceConnectionException {
        try {
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
                    throw new ResourceConnectionException("Cannot delete resource - HTTP method not allowed (405)");
                default:
                    throw new ResourceConnectionException("Cannot delete resource - status code " + responseCode);
            }
        } catch (IOException ioe) {
            throw new ResourceConnectionException(ioe);
        }
    }

    public URL post(T t) throws ResourceConnectionException {
        try {
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
                    throw new ResourceConnectionException("Cannot create resource - HTTP method not allowed (405)");
                default:
                    throw new ResourceConnectionException("Cannot create resource - status code " + responseCode);
            }        
            return new URL(conn.getHeaderField("Location"));
        } catch (IOException ioe) {
            throw new ResourceConnectionException(ioe);
        } catch (JAXBException jaxbe) {
            throw new ResourceConnectionException(jaxbe);
        }
    }

    public URL postAny(Object obj) throws ResourceConnectionException {
        try {
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
                    throw new ResourceConnectionException("Cannot create resource - HTTP method not allowed (405)");
                default:
                    throw new ResourceConnectionException("Cannot create resource - staus code " + responseCode);
            }
            return new URL(conn.getHeaderField("Location"));
        } catch (IOException ioe) {
            throw new ResourceConnectionException(ioe);
        } catch (JAXBException jaxbe) {
            throw new ResourceConnectionException(jaxbe);
        }
    }
}
