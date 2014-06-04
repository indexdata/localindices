package com.indexdata.masterkey.localindices.notification;

public interface Sender {
  
  void send(Notification msg) throws NotificationException;
  void send(String recicipents, Notification msg) throws NotificationException;
  String getDefaultRecipients();
  String getDefaultFrom();

}
