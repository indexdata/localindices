package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

/* package ; */

import java.io.File;
//import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

// import java.lang.NoSuchFieldException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.HashMap;
// import javax.xml.parsers.ParserConfigurationException;
// import javax.xml.transform.TransformerException;
// import org.xml.sax.SAXException;

//import java.util.Date;
//import ORG.oclc.oai.harvester2.verb.*;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import java.util.logging.Level;
import java.util.logging.Logger;
//import org.apache.log4j.Logger;

public class XmlFileStorage implements HarvestStorage {

    public static final String STORAGE_TYPE = "XML_FILE"; 

    public XmlFileStorage (String xmlDir) {

        if (xmlDir == null) 
            throw new IllegalArgumentException("XmlFileStorage: " 
                                               + "expect 'xmlDir' parameter");

        this.xmlDir = new File(xmlDir);
        
        if (!this.xmlDir.exists())
            this.xmlDir.mkdirs();

 
        if (!this.xmlDir.isDirectory())
            throw new IllegalArgumentException("XmlFileStorage: " 
                                               + this.xmlDir 
                                               + " is not directory");
       


    }

    // HarvestStorage overrides
    public void store(HarvestJob job, String content) throws IOException { 
        this.xmlFilePath = new File(this.xmlDir, 
                                    job.baseURL()
                                    + File.separatorChar + job.type()
                                    + File.separatorChar + job.part()
                                    + File.separatorChar + job.batch());

        logger.log(Level.INFO, this.xmlFilePath.getPath());
        
        if (!this.xmlFilePath.exists()){
            File xmlPath = this.xmlFilePath.getParentFile();
            //logger.debug(xmlPath);

            if (!xmlPath.exists())
                xmlPath.mkdirs();

            if (!xmlPath.isDirectory())
                throw new IllegalArgumentException("XmlFileStorage: " 
                                                   + xmlPath 
                                                   + " is not directory");
            
            this.xmlFilePath.createNewFile();
        }

        if (!this.xmlFilePath.isFile())
            throw new IllegalArgumentException("XmlFileStorage: " 
                                               + this.xmlFilePath 
                                               + " is not file");

        this.xmlFilePath.setReadable(true);
        this.xmlFilePath.setWritable(true, true);

        //DataOutputStream fos 
        //    = new DataOutputStream(new FileOutputStream(this.xmlFilePath));
        FileOutputStream fos = new FileOutputStream(this.xmlFilePath);
        fos.write(content.getBytes());
        fos.flush();
        fos.close();
        
    };

        
    private static Logger logger = Logger.getLogger(XmlFileStorage.class.getCanonicalName());

    private File xmlDir = null;
    private File xmlFilePath = null;
    //private Date start = new Date();
}
