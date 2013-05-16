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

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;
import com.indexdata.masterkey.localindices.scheduler.JobInfo;
import com.indexdata.masterkey.localindices.scheduler.SchedulerThread;

/**
 * A simple harvest scheduler status Web Service.
 * 
 * @author jakub
 */
public class OaiPmhMockupServlet extends HttpServlet {

  /**
	 * 
	 */
  private Dispatcher dispatcher; //  = new MockupDispather(); 
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
    OaiPmhHandler handler = dispatcher.onRequest(request);
    
    OaiPmhResponse oaiPmhResponse = handler.handle(request);
    
    //TODO Need to be able to handle encoding as well
    response.setContentType("text/xml;charset=UTF-8");
    PrintWriter out = response.getWriter();
    //TODO Need to be able to handle gzip data 
    out.write(oaiPmhResponse.toString());
    
  }

  public Dispatcher getDispatcher() {
    return dispatcher;
  }

  public void setDispatcher(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

}
