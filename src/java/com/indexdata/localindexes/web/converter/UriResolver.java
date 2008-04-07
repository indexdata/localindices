/*
 * UriResolver.java
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.converter;

import javax.ws.rs.WebApplicationException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.xml.bind.JAXBContext;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.UserTransaction;

/*import com.indexdata.localindexes.web.service.PersistenceService;*/


/**
 * Utility class for resolving an uri into an entity.
 *
 * @author jakub
 */
public class UriResolver {
    
     /** Persistence stuff */
    @PersistenceUnit(unitName = "localindexes")
    private EntityManagerFactory emf;

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    @Resource
    private UserTransaction utx;
    /* persistence stuff ends */
    
    private static ThreadLocal<UriResolver> instance = new ThreadLocal<UriResolver>() {
        protected UriResolver initialValue() {
            return new UriResolver();
        }
    };
    
    private boolean inProgress = false;
    
    private UriResolver() {
    }
    
    /**
     * Returns an instance of UriResolver.
     *
     * @return an instance of UriResolver.
     */
    public static UriResolver getInstance() {
        return instance.get();
    }
    
    private static void removeInstance() {
        instance.remove();
    }
    
    /**
     * Returns the entity associated with the given uri.
     *
     * @param type the converter class used to unmarshal the entity from XML
     * @param uri the uri identifying the entity
     * @return the entity associated with the given uri
     */
    public <T> T resolve(Class<T> type, URI uri) {
        if (inProgress) return null;
        
        inProgress = true;
        
        try {
            if (uri == null) {
                throw new RuntimeException("No uri specified in a reference.");
            }
            
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                JAXBContext context = JAXBContext.newInstance(type);
                Object obj = context.createUnmarshaller().unmarshal(conn.getInputStream());
                resolveEntity(obj);
                
                return (T) obj;
            } else {
                throw new WebApplicationException(new Throwable("Resource for " + uri + " does not exist."), 404);
            }
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        } finally {
            removeInstance();
        }
    }
    
    private void resolveEntity(Object obj) {
        EntityManager em = getEntityManager();
        try {
            Method method = obj.getClass().getMethod("getEntity");
            Object entity = method.invoke(obj);
            utx.begin();
            em.joinTransaction();
            entity = em.merge(entity);
            em.refresh(entity);
            utx.commit();
            /*entity = PersistenceService.getInstance().resolveEntity(entity);*/
            method = obj.getClass().getMethod("setEntity", entity.getClass());
            method.invoke(obj, entity.getClass().cast(entity));
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        } finally {
            em.close();
        }
    }
}
