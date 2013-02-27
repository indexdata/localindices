package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;

class Transformer implements ContentHandler{
  @SuppressWarnings("rawtypes")
  private Stack valueStack;
  
  public Object getResult(){
      if(valueStack == null || valueStack.size() == 0)
          return null;
      return valueStack.peek();
  }
  
  public boolean endArray () throws ParseException, IOException {
      trackBack();
      return true;
  }

  public void endJSON () throws ParseException, IOException {}

  public boolean endObject () throws ParseException, IOException {
      trackBack();
      return true;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public boolean endObjectEntry () throws ParseException, IOException {
      Object value = valueStack.pop();
      Object key = valueStack.pop();
      Map parent = (Map) valueStack.peek();
      parent.put(key, value);
      return true;
  }

  @SuppressWarnings("unchecked")
  private void trackBack(){
      if(valueStack.size() > 1){
          Object value = valueStack.pop();
          Object prev = valueStack.peek();
          if(prev instanceof String){
              valueStack.push(value);
          }
      }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void consumeValue(Object value){
      if(valueStack.size() == 0)
          valueStack.push(value);
      else{
          Object prev = valueStack.peek();
          if(prev instanceof List){
              List array = (List)prev;
              array.add(value);
          }
          else{
              valueStack.push(value);
          }
      }
  }
  
  public boolean primitive (Object value) throws ParseException, IOException {
      consumeValue(value);
      return true;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public boolean startArray () throws ParseException, IOException {
      List array = new JSONArray();
      consumeValue(array);
      valueStack.push(array);
      return true;
  }

  @SuppressWarnings("rawtypes")
  public void startJSON () throws ParseException, IOException {
      valueStack = new Stack();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public boolean startObject () throws ParseException, IOException {
      Map object = new JSONObject();
      consumeValue(object);
      valueStack.push(object);
      return true;
  }

  @SuppressWarnings("unchecked")
  public boolean startObjectEntry (String key) throws ParseException, IOException {
      valueStack.push(key);
      return true;
  }
  
}
