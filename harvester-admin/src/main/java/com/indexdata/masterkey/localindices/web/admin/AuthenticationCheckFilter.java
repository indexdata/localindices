/*
 * Copyright (c) 1995-2011, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.admin;

import java.io.IOException;

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

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.web.admin.controller.LoginManager;

/**
 * 
 * @author jakub
 */
public class AuthenticationCheckFilter implements Filter {
  private static String LOGIN_PAGE;
  private static String SU_COOKIE_NAME;
  private static String USER_TORUS_URI;

  public void init(FilterConfig cfg) throws ServletException {
    LOGIN_PAGE = cfg.getInitParameter("LOGIN_PAGE");
    if (LOGIN_PAGE == null)
      throw new UnavailableException("Missing init parameter: LOGIN_PAGE");
    SU_COOKIE_NAME = cfg.getInitParameter("SU_COOKIE_NAME");
    USER_TORUS_URI = cfg.getInitParameter("USER_TORUS_URI");
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    HttpSession session = req.getSession();
    String pageRequested = req.getRequestURI();
    // if the loginManager instance is created by faces, the parameters are stored in the request
    if (pageRequested == null || !pageRequested.endsWith(LOGIN_PAGE)) {
      LoginManager logMgr = (LoginManager) session.getAttribute("loginManager");
      if (logMgr == null) {
	Logger.getLogger(this.getClass()).warn("Initializing LoginManager from web context. Not faces-context");
	logMgr = new LoginManager();
	logMgr.setUserTorusURI(USER_TORUS_URI);
	logMgr.setUserTorusURI("dummy");
	session.setAttribute("loginManager", logMgr);
      }
      if (logMgr.getUserTorusURI() == null) {
	Logger.getLogger(this.getClass()).warn("No User Torus URI set. Missing in faces-context?");
	logMgr.setUserTorusURI(USER_TORUS_URI);
      }
      // logged in locally, good enough
      if (logMgr.isLoggedIn()) {
	chain.doFilter(request, response);
	return;
      }
      // otherwise go for cookie
      String suId = getCookieValue(req, SU_COOKIE_NAME);
      if (suId != null) {
	// try to login, using that cookie
	if (logMgr.doLoginWithId(suId)) {
	  chain.doFilter(request, response);
	  return;
	}
      }
      // block access
      res.sendRedirect(LOGIN_PAGE);
      return;
    }
    chain.doFilter(request, response);
  }

  public void destroy() {
  }

  private String getCookieValue(HttpServletRequest req, String cookieName) {
    if (req.getCookies() == null)
      return null;
    for (Cookie cookie : req.getCookies()) {
      if (cookie.getName().equals(cookieName)) {
	return cookie.getValue();
      }
    }
    return null;
  }

  @SuppressWarnings("unused")
  private void removeCookieByName(HttpServletResponse res, String name) {
    Cookie rotten = new Cookie(name, "");
    rotten.setMaxAge(0);
    res.addCookie(rotten);
  }
}