
/**
 Copyright 2006 OCLC, Online Computer Library Center
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ORG.oclc.oai.harvester2.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import ORG.oclc.oai.harvester2.verb.ListSets;

public class RawWrite {
  static Logger logger = Logger.getLogger(RawWrite.class);
    public static void main(String[] args) {
	try {
	    System.out.println(new Date());
	    
            HashMap<String, Object> options = getOptions(args);
            @SuppressWarnings("unchecked")
			List<String> rootArgs = (List<String>)options.get("rootArgs");
            String baseURL = null;
            if (rootArgs.size() > 0) {
                baseURL = (String)rootArgs.get(0);
            } else {
                throw new IllegalArgumentException();
            }
            
            OutputStream out = System.out;
            String outFileName = (String)options.get("-out");
            String from = (String)options.get("-from");
            String until = (String)options.get("-until");
            String metadataPrefix = (String)options.get("-metadataPrefix");
            if (metadataPrefix == null) metadataPrefix = "oai_dc";
            String resumptionToken = (String)options.get("-resumptionToken");
            String setSpec = (String)options.get("-setSpec");

            if (resumptionToken != null) {
                if (outFileName != null)
                    out = new FileOutputStream(outFileName, true);
                run(baseURL, resumptionToken, out);
            } else {
                if (outFileName != null)
                    out = new FileOutputStream(outFileName);
                run(baseURL, from, until, metadataPrefix, setSpec, out);
            }
            
            if (out != System.out) out.close();
	    System.out.println(new Date());
        } catch (IllegalArgumentException e) {
            System.err.println("RawWrite <-from date> <-until date> <-metadataPrefix prefix> <-setSpec setName> <-resumptionToken token> <-out fileName> baseURL");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    public static void run(String baseURL, String resumptionToken,
                           OutputStream out)
        throws IOException, ParserConfigurationException, ResponseParsingException, TransformerException,
               NoSuchFieldException {
        ListRecords listRecords = new ListRecords(baseURL, null, logger);
        while (listRecords != null) {
          listRecords.harvest(resumptionToken, null, null); 
          NodeList errors = listRecords.getErrors();
            if (errors != null && errors.getLength() > 0) {
                System.out.println("Found errors");
                int length = errors.getLength();
                for (int i=0; i<length; ++i) {
                    Node item = errors.item(i);
                    System.out.println(item);
                }
                System.out.println("Error record: " + listRecords.toString());
                break;
            }
//             System.out.println(listRecords);
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            resumptionToken = listRecords.getResumptionToken();
            System.out.println("resumptionToken: " + resumptionToken);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
                break; 
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }

    public static void run(String baseURL, String from, String until,
                           String metadataPrefix, String setSpec,
                           OutputStream out)
        throws IOException, ParserConfigurationException, ResponseParsingException, TransformerException,
               NoSuchFieldException {
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
        out.write("<harvest>\n".getBytes("UTF-8"));
        out.write(new Identify(baseURL, null, null, logger).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        out.write(new ListMetadataFormats(baseURL, null, null, logger).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        out.write(new ListSets(baseURL, null, null, logger).toString().getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));
        ListRecords listRecords = new ListRecords(baseURL, null, logger); 
        while (listRecords != null) {
          listRecords.harvest(from, until, setSpec, metadataPrefix, null, null);
            NodeList errors = listRecords.getErrors();
            if (errors != null && errors.getLength() > 0) {
                System.out.println("Found errors");
                int length = errors.getLength();
                for (int i=0; i<length; ++i) {
                    Node item = errors.item(i);
                    System.out.println(item);
                }
                System.out.println("Error record: " + listRecords.toString());
                break;
            }
//             System.out.println(listRecords);
            out.write(listRecords.toString().getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
            String resumptionToken = listRecords.getResumptionToken();
            System.out.println("resumptionToken: " + resumptionToken);
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
                break;
            } else {
                listRecords.harvest(resumptionToken, null, null);
            }
        }
        out.write("</harvest>\n".getBytes("UTF-8"));
    }

    private static HashMap<String, Object> getOptions(String[] args) {
        HashMap<String, Object> options = new HashMap<String, Object>();
        ArrayList<String> rootArgs = new ArrayList<String>();
        options.put("rootArgs", rootArgs);
        
        for (int i=0; i<args.length; ++i) {
            if (args[i].charAt(0) != '-') {
                rootArgs.add(args[i]);
            } else if (i+1 < args.length) {
                options.put(args[i], args[++i]);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return options;
    }
}
