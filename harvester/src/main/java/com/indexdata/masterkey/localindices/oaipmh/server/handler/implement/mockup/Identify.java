package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;
public class Identify extends CommonOaiPmhHandler {

  Map<String, String> properties = new HashMap<String, String>();

  String identify = 
      "	<Identify>\n";
  String identifyEnd = 
      "	</Identify>\n";

  String identifyExample 
  	= "    <repositoryName>Index Data Mock OAI-PMH Server Repository 1</repositoryName>\n" + 
  	  "    <baseURL>" + getBaseUrl() + "</baseURL>\n" + 
  	  "    <protocolVersion>" + getProtocolVersion() + "</protocolVersion>\n" + 
  	  "    <adminEmail>info@indexdata.com</adminEmail>\n" + 
  	  "    <adminEmail>info@indexdata.com</adminEmail>\n" + 
  	  "    <earliestDatestamp>" + getEarliestDatestamp() + "</earliestDatestamp>\n" + 
  	  "    <deletedRecord>" + getDeletedRecords() + "</deletedRecord>\n" + 
  	  "    <granularity>" + getGranularity() + "</granularity>\n" + 
  	  "    <compression>" + getCompression() + "</compression>\n" + 
  	  "    <description>\n" + 
  	  "      <oai-identifier \n" + 
  	  "        xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"\n" + 
  	  "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
  	  "        xsi:schemaLocation=\n" + 
  	  "            \"http://www.openarchives.org/OAI/2.0/oai-identifier\n" + 
  	  "        http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">\n" + 
  	  "        <scheme>oai</scheme>\n" + 
  	  "        <repositoryIdentifier>id1.indexdata.com</repositoryIdentifier>\n" + 
  	  "        <delimiter>:</delimiter>\n" + 
  	  "        <sampleIdentifier>oai:id1.indexdata.com:id/id.002</sampleIdentifier>\n" + 
  	  "      </oai-identifier>\n" + 
  	  "    </description>\n" + 
  	  "    <description>\n" + 
  	  "      <eprints \n" + 
  	  "         xmlns=\"http://www.openarchives.org/OAI/1.1/eprints\"\n" + 
  	  "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
  	  "         xsi:schemaLocation=\"http://www.openarchives.org/OAI/1.1/eprints \n" + 
  	  "         http://www.openarchives.org/OAI/1.1/eprints.xsd\">\n" + 
  	  "        <content>\n" + 
  	  "          <URL>http://www.indexdata.com/ammem/oamh/id1_content.html</URL>\n" + 
  	  "          <text>Test data for Index Data Mockup harvester\n </text>\n" + 
  	  "        </content>\n" + 
  	  "        <metadataPolicy/>\n" + 
  	  "        <dataPolicy/>\n" + 
  	  "      </eprints>\n" + 
  	  "    </description>\n" + 
  	  "    <description>\n" + 
  	  "      <friends \n" + 
  	  "          xmlns=\"http://www.openarchives.org/OAI/2.0/friends/\" \n" + 
  	  "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
  	  "          xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/friends/\n" + 
  	  "         http://www.openarchives.org/OAI/2.0/friends.xsd\">\n" + 
  	  "       <baseURL>http://oai.east.org/foo/</baseURL>\n" + 
  	  "       <baseURL>http://oai.hq.org/bar/</baseURL>\n" + 
  	  "       <baseURL>http://oai.south.org/repo.cgi</baseURL>\n" + 
  	  "     </friends>\n" + 
  	  "   </description>";

  @Override
  public OaiPmhResponse handle(OaiPmhRequest request) 
  {
    String[][] requiredParameters = {{"verb"}};
    verifyParameters(request, requiredParameters); 

    StringBuffer xml = new StringBuffer();
    xml.append(getRequest(request)).append(identify);
    xml.append(identifyExample).append(identifyEnd);
    
    return new MockupOaiPmhResponse(xml.toString()); 
  }

  private String getProtocolVersion() {
    return "2.0";
  }

  private String getBaseUrl() {      
    return "http://localhost:8080/harvester/oaipmh";
  }

  private String getCompression() {
    return "deflate";
  }

  private String getEarliestDatestamp() {
    return "1970-01-01";
  }

  private String getGranularity() {
    String[] options = { "YYYY-MM-DD", "YYYY-MM-DDThh:mm:ssZ" }; 
    return options[0];
  }

  private String getDeletedRecords() {
    String[] options = {"no", "transient", "persistent"};
    return options[0];
  }

  public String getRequest(OaiPmhRequest request) {
    String requestResponse = 
	      "	<request " 
		  + request.getParameterValue("verb") + ">"  
		  + request.getBaseUrl() 
		  + "</request>\n"; 
    return requestResponse;
  }
}
