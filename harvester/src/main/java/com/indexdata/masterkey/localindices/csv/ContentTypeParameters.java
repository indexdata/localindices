/*
 * Copyright (c) 1995-2014, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.csv;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses parameter portion of the Content-Type header, similarly to what is
 * defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7
 * @author jakub
 */
public class ContentTypeParameters {
  private String in;
  private int pos;
  
  public Map<String, String> parse(String in) throws ParseException {
    this.in = in;
    pos = 0;
    Map<String, String> params = new HashMap<String, String>();
    while (pos < in.length()) {
      parseNameValue(params);
      skipSpaceOrSemicolon();
    }
    return params;
  }
  
  private void parseNameValue(Map<String, String> params) throws ParseException {
    skipSpace();
    String name = parseName();
    skipSpace();
    parseEq();
    skipSpace();
    String value = parseValue();
    params.put(name, value);
  }
  
  private void skipSpaceOrSemicolon() {
    while (pos < in.length() 
      && (Character.isWhitespace(in.charAt(pos)) || in.charAt(pos) == ';'))
      pos++;
  }
  
  private void skipSpace() {
    while (pos < in.length() && Character.isWhitespace(in.charAt(pos)))
      pos++;
  }
  
  private void parseEq() throws ParseException {
    if (!(pos < in.length() && in.charAt(pos) == '='))
      throw new ParseException("Missing '=' at >" + in.substring(pos), pos);
    pos++;
  }
  
  private String parseName() throws ParseException {
    skipSpace();
    if (pos < in.length() 
      && in.charAt(pos) == '"' || in.charAt(pos) == '=' || in.charAt(pos) == ';')
      throw new ParseException("Unexpected " + in.charAt(pos) + " at >" + in.substring(pos), pos);
    String name = "";
    while (pos < in.length()) {
      char c = in.charAt(pos);
      if (c == '"' || in.charAt(pos) == ';')
        throw new ParseException("Unexpected " + c + " at >" + in.substring(pos), pos);
      if (Character.isWhitespace(c) || c == '=')
        return name;
      name += c;
      pos++;
    }
    return name;
  }
  
  private String parseValue() throws ParseException {
    //EOF
    if (!(pos < in.length()))
      throw new ParseException(in.substring(pos), pos);
    String value = "";
    boolean quoted = false;
    char c = in.charAt(pos);
    if (c == '"') {
      quoted = true;
      pos++;
    }
    while (pos < in.length()) {
      c = in.charAt(pos);
      if (quoted && c == '"') {
        quoted = false;
        pos++;
        break;
      }
      if (!quoted && c == '"')
        throw new ParseException("Unexpected "+c+" at "+ in.substring(pos), pos);
      if (!quoted && (Character.isWhitespace(c) || c == ';')) {
        break;
      }
      value += c;
      pos++;
    }
    //mis-quoted
    if (quoted)
      throw new ParseException("Non terminated at >" + in.substring(pos), pos);
    if (value.isEmpty())
      throw new ParseException("No value at "+in.substring(pos), pos);
    return value;
  }
  
}
