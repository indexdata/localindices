/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.web.service.client.ResourceConnector;
import com.indexdata.torus.Record;
import com.indexdata.torus.Records;
import com.indexdata.torus.layerbean.IdentityTypeLayer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.UserException;

/**
 *
 * @author jakub
 */

public class LoginManager {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
    private String idTorusURI = "http://us4java.indexdata.com/torus/admin/records/admin/";
    private String username;
    private String password;
    private boolean loggedIn = false;

    public boolean isLoggedIn() {
        return loggedIn;
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
    
    public boolean doLoginWithId(String id) {
          try {
            String url = idTorusURI + id;
            logger.log(Level.INFO, "Connecting to the identity torus at " + url);
            ResourceConnector<Record> torusConn = new ResourceConnector<Record>(new URL(url), "com.indexdata.torus.layerbean" + ":com.indexdata.torus");
            Record record = torusConn.get();
            if (record == null) {
                loggedIn = false;
            }
            loggedIn = true;
            return true;
        } catch (Exception ex) {
            logger.log(Level.INFO, "Login failed because of the backend error", ex);
            return false;
        }
        
    }
    
    public String doLogin() {
        try {
            String url = idTorusURI + "?query=" + URLEncoder.encode("userName=" + username + " and " + "password=" + password, "UTF-8");
            logger.log(Level.INFO, "Connecting to the identity torus at " + url);
            ResourceConnector<Records> torusConn = new ResourceConnector<Records>(new URL(url), "com.indexdata.torus.layerbean" + ":com.indexdata.torus");
            Records records = torusConn.get();
            if (records.getRecords() == null || records.getRecords().isEmpty()) {
                return "login_fail";
            }
            loggedIn = true;
            return "list_resources";
        } catch (Exception ex) {
            logger.log(Level.INFO, "Login failed because of the backend error", ex);
            return "login_backend_fail";
        }
    }
    
    
    public String doLogout() {
        username = password = null;
        loggedIn  = false;
        return "logged_out";
    }

    /** Creates a new instance of LoginManager */
    public LoginManager() {
    }

}
