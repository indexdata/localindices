package com.indexdata.masterkey.localindices.harvest.job;

public class MimeTypeCharSet {
  private final static String CHARSET_PREFIX =  "charset=";
  private String mimetype = "";
  private String charset  = null;
  public MimeTypeCharSet(String mimetypeCharset) {
    if (mimetypeCharset == null)
      	return ;
    int pos = mimetypeCharset.indexOf(";");
    if (pos > 0) {
      mimetype = mimetypeCharset.substring(0, pos);
      charset = mimetypeCharset.substring(pos);
      pos = charset.indexOf(CHARSET_PREFIX);
      if (pos > 0) {
	charset = charset.substring(pos+CHARSET_PREFIX.length());
      }
    }
    else 
      mimetype = mimetypeCharset;
  }

  String getMimeType() {
    return mimetype;
  }

  public boolean isMimeType(String mimetype) {
    return this.mimetype.equals(mimetype);
  }

  public String getCharset() {
    return charset;
  }
  
  

}
