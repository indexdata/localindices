package com.indexdata.masterkey.localindices.notification;

public interface Sender {
  void send(Notification notification, String contentType) throws NotificationException;
  void send(String recipients, Notification notification, String contentType) throws NotificationException;
  public String getDefaultRecipients();
  public String getDefaultFrom();
  public String getAdminUrl();
}
