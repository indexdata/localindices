/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.oaipmh.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhProcotolException;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.SimpleOaiPmhRequest;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup.MockUpDispatcher;

/**
 * A simple harvest scheduler status Web Service.
 * 
 * @author jakub
 */
public class OaiPmhMockupServlet extends HttpServlet {

  /**
	 * 
	 */
  private Dispatcher dispatcher = new MockUpDispatcher(); 
  private static final long serialVersionUID = -4732437758098313248L;

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    
    try {
      OaiPmhHandler handler = dispatcher.onRequest(request);
      OaiPmhRequest oaiPmhRequest = new SimpleOaiPmhRequest(request);  
      OaiPmhResponse oaiPmhResponse = handler.handle(oaiPmhRequest);
      //TODO Need to be able to handle encoding as well
      response.setContentType("text/xml;charset=UTF-8");
      PrintWriter out = response.getWriter();
      //TODO Need to be able to handle gzip data 
      out.write(oaiPmhResponse.toString());
    } catch (OaiPmhProcotolException e) {
      e.printStackTrace();
      response.setContentType("text/xml;charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.write(e.toString());
      // response.sendError(500, e.getMessage());
    }
  }

  public Dispatcher getDispatcher() {
    return dispatcher;
  }

  public void setDispatcher(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

}
