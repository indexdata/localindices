package com.indexdata.masterkey.localindices.harvest.job;

public class MimeTypeCharSet {
  
  private String mimetype = "";
  private String charset  = null;
  static String CHARSET =  "charset=";
  public MimeTypeCharSet(String mimetypeCharset) {
    if (mimetypeCharset == null)
      	return ;
    int pos = mimetypeCharset.indexOf(";");
    if (pos > 0) {
      mimetype = mimetypeCharset.substring(0, pos);
      charset = mimetypeCharset.substring(pos);
      pos = charset.indexOf(CHARSET);
      if (pos > 0) {
	charset = charset.substring(pos+CHARSET.length());
      }
    }
    else 
      mimetype = mimetypeCharset;
  }

  String getMimeType() {
    return mimetype;
  }

  public boolean isMimeType(String mimetype) {
    if (this.mimetype.equals(mimetype))
      return true;
    return false;
  }

  public String getCharset() {
    return charset;
  }
  
  

}
