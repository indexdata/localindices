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
 *
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
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        SchedulerThread st = (SchedulerThread) getServletContext().getAttribute("schedulerThread");
        out.println("<jobs>");
        for (JobInfo ji : st.getJobInfo()) {
            Harvestable hable = ji.getHarvestable();
            out.println("<job id=\"" + hable.getId() + "\">");
            out.println("<name>" + hable.getName() + "</name>");
            out.println("<scheduleString>" + hable.getScheduleString() + "</scheduleString>");
            out.println("<error>" + hable.getCurrentStatus() + "</error>");
            out.println("<status>" + ji.getStatus() + "</status>");
            out.println("</job>");
        }
        out.println("</jobs>");
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    /** 
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
