/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.scheduler;

import com.indexdata.localindexes.web.entity.Harvestable;
import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * A simple harvest scheduler status Web Service.
 * @author jakub
 */
public class SchedulerStatusServlet extends HttpServlet {

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
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
