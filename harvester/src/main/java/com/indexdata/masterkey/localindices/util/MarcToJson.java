package com.indexdata.masterkey.localindices.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.json.simple.JSONObject;
import org.marc4j.*;
import org.marc4j.marc.Record;
import org.xml.sax.SAXException;


public class MarcToJson {

  public static List<JSONObject> convertMarcRecordsToJson(InputStream inputStream)
      throws SAXException, IOException, ParserConfigurationException {
    List<JSONObject> jsonList = new ArrayList<>();
    MarcReader reader = new MarcPermissiveStreamReader(inputStream, true, true);
    while(reader.hasNext()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Record record = reader.next();
      MarcWriter writer = new MarcXmlWriter(baos);
      writer.write(record);
      writer.close();
      JSONObject json = MarcXMLToJson.convertMarcXMLToJson(baos.toString("UTF-8"));
      jsonList.add(json);
    }
    return jsonList;
  }

  public static List<JSONObject> convertMarcRecordsToJson(byte[] marc)
      throws SAXException, IOException, ParserConfigurationException {
    return convertMarcRecordsToJson(new ByteArrayInputStream(marc));
  }

}
