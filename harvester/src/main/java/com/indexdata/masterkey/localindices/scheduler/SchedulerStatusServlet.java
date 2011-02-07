/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;

/**
 * A simple harvest scheduler status Web Service.
 * @author jakub
 */
public class SchedulerStatusServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = -4732437758098313248L;

	/** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SchedulerThread st = (SchedulerThread) getServletContext().getAttribute("schedulerThread");
        dispatchRequest(request, st);
        
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<jobs>");
        for (JobInfo ji : st.getJobInfo()) {
            Harvestable hable = ji.getHarvestable();
            out.println("<job id=\"" + hable.getId() + "\">");
            out.println("<name>" + hable.getName() + "</name>");
            out.println("<scheduleString>" + hable.getScheduleString() + "</scheduleString>");
            out.println("<lastUpdated>" + hable.getLastUpdated() + "</lastUpdated>");
            out.println("<lastHarvested>" + hable.getLastHarvestStarted() + "</lastHarvested>");
            out.println("<reportedStatus>" + hable.getCurrentStatus() + "</reportedStatus>");
            out.println("<latestStatus>" + ji.getStatus() + "</latestStatus>");
            out.println("<error>" + ji.getError() + "</error>");
            if(hable instanceof OaiPmhResource) {
                OaiPmhResource oaiHable = (OaiPmhResource) hable;
                out.println("<typeSpecific type=\"OaiPmhResource\">");
                out.println("<url>" + oaiHable.getUrl() + "</url>");
                out.println("<metadataPrefix>" + oaiHable.getMetadataPrefix() + "</metadataPrefix>");
                out.println("<setName>" + oaiHable.getOaiSetName() + "</setName>");
                out.println("<fromDate>" + oaiHable.getFromDate() + "</fromDate>");
                out.println("<untilDate>" + oaiHable.getUntilDate() + "</untilDate>");
                out.println("<dateFormat>" + oaiHable.getDateFormat() + "</dateFormat>");
                out.println("</typeSpecific>");
            }                
            out.println("</job>");
        }
        out.println("</jobs>");
    }
    
    private void dispatchRequest(HttpServletRequest req, SchedulerThread st) {
        if (req.getParameter("shutdown") != null)
            st.kill();
        else if (req.getParameter("kill_job") != null)
            st.stopJob(Long.parseLong(req.getParameter("kill_job")));
    }
}
