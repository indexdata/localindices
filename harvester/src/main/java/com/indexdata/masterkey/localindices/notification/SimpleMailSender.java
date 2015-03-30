package com.indexdata.masterkey.localindices.notification;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SimpleMailSender implements Sender {
  private final String defaultFrom;
  private final String defaultRecipients;
  private final String smtpServer;
  private final String adminUrl;
  private Properties additionalProperties = null;
  private String debug = null;
  private String ssl = null;
  private String portStr = null;
  
  public SimpleMailSender(String from, String defaultRecipients,
    String smtpServer, String adminUrl) {
    this.defaultFrom = from;
    this.defaultRecipients = defaultRecipients;
    this.smtpServer = smtpServer;
    this.adminUrl = adminUrl;
  }
  
  public SimpleMailSender(String from, String defaultRecipients,
      String smtpServer, String adminUrl, Properties properties) {
      this.defaultFrom = from;
      this.defaultRecipients = defaultRecipients;
      this.smtpServer = smtpServer;
      this.adminUrl = adminUrl;
      if (properties != null) {
        this.additionalProperties = properties;
        debug = additionalProperties.getProperty("harvester.smtp.debug");
        ssl = additionalProperties.getProperty("harvester.smtp.ssl");
        portStr = additionalProperties.getProperty("harvester.smtp.port");
      }
    }

 
  private void sendViaSMTP(String smtpServer, String recipients, String from,
    String subject, String body, String contentType) throws NotificationException {
    try {
      Properties props = System.getProperties();
      // -- Attaching to default Session, or we could start a new one --
      props.put("mail.smtp.host", smtpServer);
      if (debug != null && debug.equals("true")) props.put("mail.debug", "true");
      Session session = Session.getDefaultInstance(props, null);
      // -- Create a new message --
      Message msg = new MimeMessage(session);
      // -- Set the FROM and TO fields --
      msg.setFrom(new InternetAddress(from));
      msg.setRecipients(Message.RecipientType.TO,
        InternetAddress.parse(recipients, false));
     // -- We could include CC recipients too --
      // if (cc != null)
      // msg.setDefaultRecipients(Message.RecipientType.CC
      // ,InternetAddress.parse(cc, false));
      // -- Set the subject and body text --
      msg.setSubject(subject);
      msg.setContent(body, contentType);
      // -- Set some other header information --
      msg.setHeader("X-Mailer", "IndexData Harvester");
      msg.setSentDate(new Date());
      if (ssl != null && ssl.equals("true")) {
        Transport transport = session.getTransport("smtps");
        if (additionalProperties.get("harvester.smtp.username") != null) {
          String username = additionalProperties.getProperty("harvester.smtp.username");
          String password = additionalProperties.getProperty("harvester.smtp.password");
          int port = 465;
          if (portStr != null) {
            try { port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) { /* keep default port */ }
          }
          transport.connect(smtpServer,port,username,password);
        } else {
          transport.connect();
        }
        msg.saveChanges();
        // -- Send the message --
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();
      } else {
        // -- Send the message --
        Transport.send(msg);
      }
    } catch (MessagingException e) {
      throw new NotificationException("Unable to send notification (smtp="
        + smtpServer+" to="+recipients+" from="+from+"): "
        + e.getMessage(), e);
    }
  }

  @Override
  public void send(Notification notification, String contentType) throws NotificationException {
    sendViaSMTP(smtpServer, defaultRecipients, defaultFrom, 
      notification.getStatus() + ": "+ notification.getSubject(), 
      notification.getMesage(), contentType);
  }

  @Override
  public void send(String recipients, Notification notification, String contentType) throws
    NotificationException {
    sendViaSMTP(smtpServer, recipients, defaultFrom, 
      notification.getStatus() + ": "+notification.getSubject(),
      notification.getMesage(), contentType);
  }

  @Override
  public String getDefaultFrom() {
    return defaultFrom;
  }

  @Override
  public String getDefaultRecipients() {
    return defaultRecipients;
  }

  public String getSmtpServer() {
    return smtpServer;
  }

  @Override
  public String getAdminUrl() {
    return adminUrl;
  }

}
