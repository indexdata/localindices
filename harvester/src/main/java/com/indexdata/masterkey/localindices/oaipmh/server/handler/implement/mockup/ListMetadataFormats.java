package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.ListRecordsHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;
public class ListMetadataFormats extends CommonOaiPmhHandler implements ListRecordsHandler {

  Map<String, String> properties = new HashMap<String, String>();

  String data =
      "	  <metadataFormat>\n" + 
      "     <metadataPrefix>oai_dc</metadataPrefix>\n" + 
      "     <schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd\n" + 
      "       </schema>\n" + 
      "     <metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       </metadataNamespace>\n" + 
      "   </metadataFormat>\n" + 
      "   <metadataFormat>\n" + 
      "     <metadataPrefix>olac</metadataPrefix>\n" + 
      "     <schema>http://www.language-archives.org/OLAC/olac-0.2.xsd</schema>\n" + 
      "     <metadataNamespace>http://www.language-archives.org/OLAC/0.2/\n" + 
      "      </metadataNamespace>\n" + 
      "   </metadataFormat>\n" + 
      "   <metadataFormat>\n" + 
      "     <metadataPrefix>perseus</metadataPrefix>\n" + 
      "     <schema>http://www.perseus.tufts.edu/persmeta.xsd</schema>\n" + 
      "     <metadataNamespace>http://www.perseus.tufts.edu/persmeta.dtd\n" + 
      "       </metadataNamespace>\n" + 
      "   </metadataFormat>\n";
  
  private String resumptionTokenStart = "<resumptionToken>";
  private String resumptionTokenEnd = "</resumptionToken>"; 

  @SuppressWarnings("unused")
  @Override
  public OaiPmhResponse handle(OaiPmhRequest request) {

    String[][] requiredParameters = { {"verb", "identifier"}, {"verb"} };
    verifyParameters(request, requiredParameters);
    
    loadSetData(request);
    
    StringBuffer xml = new StringBuffer()
    		.append(getRequest(request))
		.append(getElement(request))
    //    TODO Implement a Set Generator.
		.append(data);

    String resumptionToken = null; // generator.generateRecords(xml);
    if (resumptionToken != null) {
      xml.append(resumptionTokenStart + resumptionToken  + resumptionTokenEnd);
    }
    xml.append(getElementEnd(request));
    return new MockupOaiPmhResponse(xml.toString()); 
  }

  private void loadSetData(OaiPmhRequest request) 
  {
    String setValue = request.getParameter("set");
    if (setValue == null)
      return; 
    
    InputStream inputStream = getClass().getResourceAsStream(setValue);
    if (inputStream == null) {
      File setFile = new File(setValue);
      try {
	inputStream = new FileInputStream(setFile);
      } catch (FileNotFoundException e) {
	e.printStackTrace();
	throw new RuntimeException("Set '" + setValue + "' definition not found!"); 
      }
    }
    Reader reader = new InputStreamReader(inputStream);
    BufferedReader lineReader = new BufferedReader(reader);
    try {
      while (lineReader.ready()) {
	String line = lineReader.readLine();
	if (line == null) 
	  break;
	
      }
    }
    catch(IOException ioe) {
      
    }
  }

  public void verifyParameters(OaiPmhRequest request, String[][] parameters) {
    /*
    if (request.getParameter("resumptionToken") != null && request.getParameter("verb") != null) {
      return ;
    }
    */
    RuntimeException missingParameter = null; 
    for (String[] oneOption: parameters) {
      missingParameter = null; 
      for (String parameter : oneOption) 
	if (request.getParameter(parameter) == null)
	  missingParameter = new RuntimeException("Required parameter '" + parameter + "' missing");
      // Had all parameters in this one. 
      if (missingParameter == null)
	return ; 
    }
    // Order of parameter is important. Last one will determine the error message. 
    if (missingParameter != null)
      throw missingParameter;
  }

  public String getRequest(OaiPmhRequest request) {
    String requestResponse = 
	      "	<request " 
		  + request.getParameterValue("verb")  + " "
		  + request.getParameterValue("identifier") + ">"
		  + request.getBaseUrl() 
		  + "</request>\n"; 
    return requestResponse;
  }
}