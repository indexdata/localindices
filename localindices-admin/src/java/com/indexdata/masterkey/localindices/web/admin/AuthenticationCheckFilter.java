/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.admin;

import com.indexdata.masterkey.localindices.web.admin.controller.LoginManager;
import java.io.*;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author jakub
 */
public class AuthenticationCheckFilter implements Filter {
    private static String LOGIN_PAGE;

    public void init(FilterConfig cfg) throws ServletException {
        LOGIN_PAGE = cfg.getInitParameter("LOGIN_PAGE");
        if (LOGIN_PAGE == null)
            throw new UnavailableException("Missing init parameter: LOGIN_PAGE");            
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        HttpSession session = req.getSession();
        String pageRequested = req.getRequestURI();
        
        if (pageRequested == null || !pageRequested.endsWith(LOGIN_PAGE)) {
            LoginManager logMgr = (LoginManager) session.getAttribute("loginManager");
            // first check if logged in from harvester admin
            if (logMgr == null || !logMgr.isLoggedIn()) {
                // so we're not logged in - look for the 
                for (Cookie cookie : req.getCookies()) {
                    if (cookie.getName().equals("admin-superuser")) {
                        if (logMgr.doLoginWithId(cookie.getValue()))  {
                            chain.doFilter(request, response);
                            return;
                        } else {
                            res.sendRedirect(LOGIN_PAGE);
                            return;
                        }
                    }
                }
                res.sendRedirect(LOGIN_PAGE);
                return;
            }                
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
    
}