package com.indexdata.masterkey.localindices.notification;

public interface Sender {
  
  void send(Notification msg) throws NotificationException;

}
