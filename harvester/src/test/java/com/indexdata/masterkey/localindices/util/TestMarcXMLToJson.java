package com.indexdata.masterkey.localindices.util;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import static junit.framework.Assert.assertTrue;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.xml.sax.SAXException;
import static junit.framework.Assert.assertTrue;

public class TestMarcXMLToJson {

  @Test
  public void testConvertXMLToJson() throws SAXException, IOException, ParserConfigurationException {
    String input =
        "<record xmlns=\"http://www.loc.gov/MARC21/slim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        "xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">" +
        "          <leader>01914cam a2200493Ia 4500</leader>" +
        "          <datafield tag=\"100\">" +
        "            <subfield code=\"a\">Author, Personal.</subfield>" +
        "            <subfield code=\"e\">aut</subfield>" +
        "          </datafield>" +
        "          <datafield tag=\"110\">" +
        "            <subfield code=\"a\">Author, Corporate.</subfield>" +
        "            <subfield code=\"4\">author</subfield>" +
        "          </datafield>" +
        "          <datafield tag=\"111\">" +
        "            <subfield code=\"a\">Illustrator, Meeting.</subfield>" +
        "            <subfield code=\"e\">illustrator</subfield>" +
        "          </datafield>" +
        "          <datafield ind1=\"1\" ind2=\"0\" tag=\"245\">" +
        "            <subfield code=\"a\">Instance of type 'text'</subfield>" +
        "          </datafield>" +
        "</record>";
    JSONObject output = MarcXMLToJson.convertMarcXMLToJson(input);
    String leader = (String)output.get("leader");
    String originalLeader = "01914cam a2200493Ia 4500";
    assertTrue(originalLeader.equals(leader));
  }

}
