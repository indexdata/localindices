package com.indexdata.masterkey.localindices.harvest.job;

public class MimeTypeCharSet {
  private final static String CHARSET_PREFIX =  "charset=";
  private String mimeType = "";
  private String charset;
  
  public MimeTypeCharSet(String contentType) {
    if (contentType == null) return;
    int colon = contentType.indexOf(";");
    if (colon > 0) {
      mimeType = contentType.substring(0, colon);
      charset = contentType.substring(colon);
      int prefix = charset.indexOf(CHARSET_PREFIX);
      if (prefix > 0) {
	charset = charset.substring(prefix+CHARSET_PREFIX.length());
      }
    } else { 
      mimeType = contentType;
    }
  }

  public String getMimeType() {
    return mimeType;
  }

  public boolean isMimeType(String contentType) {
    return mimeType.equals(contentType);
  }

  public String getCharset() {
    return charset;
  }
  
  @Override
  public String toString() {
    return mimeType + (charset != null ? "; charset=" + charset : "");
  }
  
}
