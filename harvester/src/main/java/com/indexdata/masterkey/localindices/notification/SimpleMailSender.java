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

  public SimpleMailSender(String from, String defaultRecipients,
    String smtpServer) {
    this.defaultFrom = from;
    this.defaultRecipients = defaultRecipients;
    this.smtpServer = smtpServer;
  }
 
  private static void sendViaSMTP(String smtpServer, String recipients, String from,
    String subject, String body) throws NotificationException {
    try {
      Properties props = System.getProperties();
      // -- Attaching to default Session, or we could start a new one --
      props.put("mail.smtp.host", smtpServer);
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
      msg.setText(body);
      // -- Set some other header information --
      msg.setHeader("X-Mailer", "IndexData Harvester");
      msg.setSentDate(new Date());
      // -- Send the message --
      Transport.send(msg);
    } catch (MessagingException e) {
      throw new NotificationException("Unable to send notification (smtp="
        + smtpServer+" to="+recipients+" from="+from+"): "
        + e.getMessage(), e);
    }
  }

  @Override
  public void send(Notification notification) throws NotificationException {
    sendViaSMTP(smtpServer, defaultRecipients, defaultFrom, 
      notification.getStatus() + ": "+ notification.getSubject(), 
      notification.getMesage());
  }

  @Override
  public void send(String recipients, Notification notification) throws
    NotificationException {
    sendViaSMTP(smtpServer, recipients, defaultFrom, 
      notification.getStatus() + ": "+notification.getSubject(),
      notification.getMesage());
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

}
