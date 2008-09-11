/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.web.service.client.ResourceConnectionException;
import com.indexdata.masterkey.localindices.web.service.client.ResourceConnector;
import com.indexdata.torus.Records;
import com.indexdata.torus.layerbean.IdentityTypeLayer;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jakub
 */

public class LoginManager {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
    private String idTorusURI = "http://us4java.indexdata.com/torus/admin/records/admin/";
    private String username;
    private String password;
    private String displayName;
    private boolean loggedIn = false;
    private String id;    
    
    /** Creates a new instance of LoginManager */
    public LoginManager() {     
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public boolean isLoggedInWithId(String suId) {
        if (!isLoggedIn() || id == null) return false;
        return id.equals(suId);
    }
    
    public boolean doLoginWithId(String id) {
          try {
            IdentityTypeLayer layer = retrieveLayer("id=" + id);
            if (layer == null) {
                loggedIn = false;
                return false;
            }
            this.id = id;
            displayName = layer.getDisplayName();
            loggedIn = true;
            return true;
        } catch (Exception ex) {
            loggedIn = false;
            logger.log(Level.INFO, "Login failed because of the backend error", ex);
            return false;
        }
        
    }
    
    public String doLogin() {
        try {
            IdentityTypeLayer layer = retrieveLayer("userName=" + username + " and " + "password=" + password);
            if (layer == null) {
                loggedIn = false;
                return "login_fail";
            }
            id = layer.getId();
            displayName = layer.getDisplayName();
            loggedIn = true;
            setCookie("admin-superuser", id, false);
            return "list_resources";
        } catch (Exception ex) {
            loggedIn = false;
            logger.log(Level.INFO, "Login failed because of the backend error", ex);
            return "login_backend_fail";
        }
    }
    
    public String doLogout() {
        loggedIn  = false;
        setCookie("admin-superuser", id, true);
        id = username = password = null;
        return "logged_out";
    }
    
    private IdentityTypeLayer retrieveLayer(String query) throws UnsupportedEncodingException, MalformedURLException, ResourceConnectionException {
        String url = idTorusURI + "?query=" + URLEncoder.encode(query, "UTF-8");
        logger.log(Level.INFO, "Connecting to the identity torus - " + url);
        ResourceConnector<Records> torusConn = new ResourceConnector<Records>(new URL(url), "com.indexdata.torus.layerbean" + ":com.indexdata.torus");
        Records records = torusConn.get();
        if (records.getRecords() == null || records.getRecords().isEmpty()) {
            return null;
        }
        return (IdentityTypeLayer) records.getRecords().iterator().next().getLayers().get(0);
    }
    
    private static void setCookie (String name, String value, boolean kill) {
        Cookie cookie = new Cookie(name, value);
        if (kill) cookie.setMaxAge(0);
        ((HttpServletResponse) FacesContext
                .getCurrentInstance()
                .getExternalContext().getResponse()).addCookie(cookie);
    }    
}
