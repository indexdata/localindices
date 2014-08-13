package com.indexdata.masterkey.localindices.harvest.job;

public class MimeTypeCharSet {
  private final static String CHARSET_PREFIX =  "charset=";
  private String mimeType = ""; //NOT NULL!
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
  
  public boolean isUndefined() {
    return mimeType.isEmpty();
  }
  
  public boolean isXML() {
    return mimeType.matches("(?:application|text)/(?:.*[+])?xml");
  }
  
  public boolean isBinary() {
    return "application/octet-stream".equals(mimeType);
  }
  
  public boolean isPlainText() {
    return "text/plain".equals(mimeType);
  }
  
  public boolean isTar() {
    return isTarGz() || "application/x-tar".equals(mimeType);
  }
  
  public boolean isTarGz() {
    return "application/x-gtar".equals(mimeType);
  }
  
  public boolean isGzip() {
    return "application/gzip".equals(mimeType)
          || "application/x-gzip".equals(mimeType)
          || "application/gzip-compressed".equals(mimeType)
          || "application/x-gzip-compressed".equals(mimeType)
          || "application/x-gunzip".equals(mimeType)
          || "application/gzipped".equals(mimeType);
  }
  
  public boolean isZip() {
    return "application/zip".equals(mimeType);
  }
  
  public boolean isHTML() {
    return "text/html".equalsIgnoreCase(mimeType) || "application/xhtml+xml".equals(mimeType);
  }
  
  public boolean isSGML() {
    return "application/sgml".equals(mimeType) || "text/sgml".equals(mimeType);
  }
  
}
