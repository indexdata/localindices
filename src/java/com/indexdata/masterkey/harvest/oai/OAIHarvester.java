package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

/* run as 

java -classpath /usr/share/java/log4j-1.2.jar:/usr/share/java/xml-apis.jar:/usr/share/java/xalan2.jar:build/jar/oaiharvester2.jar:build/jar/oaiharvester2-job.jar OAIHarvester -f 2008-03-01 -u 2008-04-01 -m oai_dc -x xml-dir http://arXiv.org/oai2

*/

/* package ; */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
//import org.apache.log4j.Logger;

import ORG.oclc.oai.harvester2.verb.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAIHarvester {
    public static void main(String[] args) {

        String baseURL = null;
        String from = null;
        String until = null;
        String metadataPrefix = null;
        String setSpec = null;
        String xmlDir = null;

	try {
            HashMap options = getOptions(args);
            List rootArgs = (List)options.get("rootArgs");

            if (rootArgs.size() > 0) {
                baseURL = (String)rootArgs.get(0);
            } 

            from = (String)options.get("-f");
            until = (String)options.get("-u");
            metadataPrefix = (String)options.get("-m");
            setSpec = (String)options.get("-s");
            xmlDir = (String)options.get("-x");

            HarvestStorage xmlStorage = new XmlFileStorage(xmlDir);

            HarvestJob oaijob 
                = new OAIHarvestJob(baseURL,
                                    from, until,
                                    metadataPrefix, setSpec); 

            oaijob.setStorage(xmlStorage);

            oaijob.run();

            //OutputStream out = System.out;
            //String outFileName = (String)options.get("-out");
            //if (out != System.out) out.close();
            

        } catch (IllegalArgumentException e) {
           logger.log(Level.INFO,"Missing argument: " + e);
           logger.log(Level.INFO,"OAIHharvester -f " + from + " -u " + until 
                               + "-m " + metadataPrefix  
                               + "-s " + setSpec + " " + " -x " + xmlDir
                               + " " + baseURL);
           logger.log(Level.INFO,"OAIHharvester -f date -u date "
                               + "-m prefix -s setName -x xmlDir baseURL");
	} catch (Exception e) {
	    //logger.log(Level.INFO,e.printStackTrace());
            e.printStackTrace();
	    System.exit(-1);
	} catch (Error e) {
	    //logger.fatal(e.printStackTrace());
            e.printStackTrace();
	    System.exit(-1);
	}
    }

    private static Logger logger = Logger.getLogger(OAIHarvestJob.class.getCanonicalName());

    private static HashMap getOptions(String[] args) {
        HashMap options = new HashMap();
        ArrayList rootArgs = new ArrayList();
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
