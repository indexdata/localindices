package com.indexdata.masterkey.localindices.notification;

public class SimpleNotification implements Notification {

  private String status, subject, message;

  public SimpleNotification(String state, String subj, String msg) {
    status = state;
    subject = subj;
    message = msg;
  }
  
  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public String getSubject() {
    return subject;
  }

  @Override
  public String getMesage() {
    return message;
    
  }

}
