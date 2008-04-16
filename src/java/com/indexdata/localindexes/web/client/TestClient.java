/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.client;

import com.indexdata.localindexes.web.entitybeans.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;


/**
 *
 * @author jakub
 */
public class TestClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Harvestable harvestable = new OaiPmhResource();
        harvestable.setName("some resource");
        harvestable.setTitle("of a given title");
        harvestable.setMaxDbSize(320);
        //marshal(harvestable);
        System.out.println(harvestable.getClass());
    }
   
    
    private static void marshal(Harvestable harvestable) {
        try {
            JAXBContext jaxbCtx = JAXBContext.newInstance(harvestable.getClass().getPackage().getName());
            Marshaller marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); //NOI18N
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(harvestable, System.out);
        } catch (JAXBException ex) {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex); //NOI18N
        }
    } 

}
