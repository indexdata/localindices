package com.indexdata.masterkey.localindices.notification;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class SenderFactory implements ServletContextListener {
  
  static SimpleMailSender sender = null;
  static ServletContext ctx;
  
  static synchronized public Sender getSender() 
  {
    if (sender == null) {
      if (ctx != null) {
	Properties props = (Properties) ctx.getAttribute("harvester.properties");
	sender = new SimpleMailSender();
	sender.setSmtpServer((String) props.getProperty("harvester.smtp.server"));
	sender.setFrom((String) props.getProperty("harvester.smtp.from"));
	sender.setRecievers((String) props.getProperty("harvester.smtp.to"));
      }
      else 
	Logger.getLogger(SenderFactory.class).error("Unable to get Servlet context");
    }
    return sender;
  }
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ctx = sce.getServletContext();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    ctx = null;
  }

}
