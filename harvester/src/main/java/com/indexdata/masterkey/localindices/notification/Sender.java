package com.indexdata.masterkey.localindices.notification;

public interface Sender {
  public void send(Notification msg) throws NotificationException;
  public void send(String recicipents, Notification msg) throws NotificationException;
  public String getDefaultRecipients();
  public String getDefaultFrom();
  public String getAdminUrl();
}
