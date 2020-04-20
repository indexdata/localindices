package com.indexdata.masterkey.localindices.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.xml.sax.SAXException;
import static junit.framework.Assert.assertTrue;

public class TestMarcToJson {
  @Test
  public void testInputStringToJson()
      throws FileNotFoundException, UnsupportedEncodingException, SAXException,
      IOException, ParserConfigurationException, URISyntaxException {
    URL res = getClass().getClassLoader().getResource("test.mrc");
    assertNotNull(res);
    File file = Paths.get(res.toURI()).toFile();
    InputStream inputStream = new FileInputStream(file.getAbsolutePath());
    List<JSONObject> jsonList = MarcToJson.convertMarcRecordsToJson(inputStream);
    assertTrue(jsonList.get(0).get("leader").equals("01201nam  2200253 a 4500"));
  }

  @Test
  public void dummyTest() {
    assertTrue(true);
  }

}
