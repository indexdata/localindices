package com.indexdata.masterkey.localindices.notification;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SimpleMailSender implements Sender {

  /**
    * A simple email sender class.
    */

  /**
      * Main method to send a message given on the command line.
      */
private String from; 
private String recievers;
private String smtpServer;

/*
  public static void main(String args[])
    {
      try
      {
        String smtpServer=args[0];
        String to=args[1];
        String from=args[2];
        String subject=args[3];
        String body=args[4];
        send(smtpServer, to, from, subject, body);
      }
      catch (Exception ex)
      {
        System.out.println("Usage: java com.lotontech.mail.SimpleSender"
         +" smtpServer toAddress fromAddress subjectText bodyText");
      }
      System.exit(0);
    }
    
*/	

/**
   * "send" method to send the message.
   * @throws NotificationException 
   */
 public static void send(String smtpServer, String to, String from
  , String subject, String body) throws NotificationException
 {
   try
   {
     Properties props = System.getProperties();
     // -- Attaching to default Session, or we could start a new one --
     props.put("mail.smtp.host", smtpServer);
     Session session = Session.getDefaultInstance(props, null);
     // -- Create a new message --
     Message msg = new MimeMessage(session);
     // -- Set the FROM and TO fields --
     msg.setFrom(new InternetAddress(from));
     msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
     // -- We could include CC recipients too --
     // if (cc != null)
     // msg.setRecipients(Message.RecipientType.CC
     // ,InternetAddress.parse(cc, false));
     // -- Set the subject and body text --
     msg.setSubject(subject);
     msg.setText(body);
     // -- Set some other header information --
     msg.setHeader("X-Mailer", "SimpleMailSender");
     msg.setSentDate(new Date());
     // -- Send the message --
     Transport.send(msg);
   }
   catch (Exception e)
   {
     throw new NotificationException("Unable to send notification: " + e.getMessage(), e);
   }
 }
  @Override
  public void send(Notification notification) throws NotificationException {
    send(smtpServer, recievers, from, notification.getStatus() + ": " + notification.getSubject(), notification.getMesage()); 
  }

  public void send(String recievers, Notification notification) throws NotificationException {
    setRecievers(recievers);
    send(notification); 
  }

  public String getFrom() {
    return from;
  }
  public void setFrom(String from) {
    this.from = from;
  }
  public String getRecievers() {
    return recievers;
  }
  public void setRecievers(String recievers) {
    this.recievers = recievers;
  }
  public String getSmtpServer() {
    return smtpServer;
  }
  public void setSmtpServer(String smtpServer) {
    this.smtpServer = smtpServer;
  }
}
