package com.indexdata.masterkey.localindicies.entity;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.SolrStorage;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StoragesConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationsConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepsConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationsConverter;
import com.indexdata.rest.client.ResourceConnector;

import junit.framework.TestCase;

public class TestDAO extends TestCase {

    /**
     * @param args the command line arguments
     */
    public void testHarvestables() throws IOException {
        try {
            System.out.println("+++ Retrieving harvestables:");
            String baseURL = "http://localhost:8080/localindices-harvester/records/";
            
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                        new URL(baseURL + "harvestables/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            HarvestablesConverter hc = harvestablesConnector.get();
            for (HarvestableBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            
            System.out.println("+++ Creating new harvestable.");
            
            Harvestable harvestable = new OaiPmhResource();
            harvestable.setName("test entry");
            harvestable.setServiceProvider("automatically posted harvestable");
            harvestable.setTechnicalNotes("relevant description");
            harvestable.setEnabled(false);
            harvestable.setCurrentStatus("no status");
            harvestable.setMaxDbSize(320);
            harvestable.setScheduleString("0:1:1");
            harvestable.setLastUpdated(new Date());
            
            HarvestableConverter harvestableContainer = new HarvestableConverter();
            harvestableContainer.setEntity(harvestable);
            URL resourceURL = harvestablesConnector.postAny(harvestableContainer);
            
            System.out.println("+++ Identifier assigned to harvestable:");
            System.out.println(resourceURL);
            
            System.out.println("+++ Retrieving the created harvestable.");
            
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            harvestable = harvestableConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved harvestable:");
            System.out.println("Harvestable id: " + harvestable.getId());
            System.out.println("Harvestable name: " + harvestable.getName());
            System.out.println("Harvestable title: " + harvestable.getServiceProvider());
            System.out.println("Harvestable description: " + harvestable.getTechnicalNotes());
            
            System.out.println("+++ Updating the harvestable with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Harvestable name: " + newName);
            System.out.println("Harvestable title: " + newTitle);
            
            Harvestable harvestableCopy = (Harvestable) harvestable.clone();
            harvestable.setName(newName);
            harvestable.setServiceProvider(newTitle);
            
            harvestableContainer.setEntity(harvestable);
            harvestableConnector.put(harvestableContainer);
            
            System.out.println("+++ Reverting the harvestable to original values.");
            harvestableContainer.setEntity(harvestableCopy);
            harvestableConnector.put(harvestableContainer);
            
            System.out.println("+++ Deleting created harvestable.");
            harvestableConnector.delete();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    
    public void testStorages() throws IOException {
        try {
            System.out.println("+++ Retrieving storages:");
            String baseURL = "http://localhost:8080/localindices-harvester/records/";
            
            ResourceConnector<StoragesConverter> storagesConnector =
                    new ResourceConnector<StoragesConverter>(
                        new URL(baseURL + "storages/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            StoragesConverter hc = storagesConnector.get();
            for (StorageBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            
            System.out.println("+++ Creating new instance ");
            
            Storage entity = new SolrStorage();
            entity.setName("Test " + entity.getClass().getCanonicalName());
            entity.setEnabled(false);
            
            StorageConverter storageConverter = new StorageConverter();
            storageConverter.setEntity(entity);
            URL resourceURL = storagesConnector.postAny(storageConverter);
            
            System.out.println("+++ Identifier assigned to storage:");
            System.out.println(resourceURL);
            
            System.out.println("+++ Retrieving the created storage.");
            
            ResourceConnector<StorageConverter> storageConnector =
                    new ResourceConnector<StorageConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            entity = storageConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved storage:");
            System.out.println("Storage id: " + entity.getId());
            System.out.println("Storage name: " + entity.getName());
            
            System.out.println("+++ Updating the storage with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Storage name: " + newName);
            System.out.println("Storage title: " + newTitle);
            
            Storage storageCopy = (Storage) entity.clone();
            entity.setName(newName);
            
            storageConverter.setEntity(entity);
            storageConnector.put(storageConverter);
            
            System.out.println("+++ Reverting the storage to original values.");
            storageConverter.setEntity(storageCopy);
            storageConnector.put(storageConverter);
            
            System.out.println("+++ Deleting created storage.");
            storageConnector.delete();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void testTransformations() throws IOException {
        try {
            System.out.println("+++ Retrieving Transformations:");
            String baseURL = "http://localhost:8080/localindices-harvester/records/";
            
            ResourceConnector<TransformationsConverter> connector =
                    new ResourceConnector<TransformationsConverter>(
                        new URL(baseURL + "transformations/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            TransformationsConverter hc = connector.get();
            for (TransformationBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            Transformation entity = new BasicTransformation();
            entity.setName("Test Transformation");
            entity.setDescription("Test Description");

            TransformationConverter converter = new TransformationConverter();
            converter.setEntity(entity);
            URL resourceURL = connector.postAny(converter);

            System.out.println("+++ Identifier assigned to entity:");
            System.out.println(resourceURL);

            System.out.println("+++ Retrieving the created entity.");
            ResourceConnector<TransformationConverter> entityConnector =
                    new ResourceConnector<TransformationConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            entity = entityConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved entity:");
            System.out.println("Id: " + entity.getId());
            System.out.println("Name: " + entity.getName());
            
            System.out.println("+++ Updating the storage with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Storage name: " + newName);
            System.out.println("Storage title: " + newTitle);
            
            Transformation copy = (Transformation) entity.clone();
            entity.setName(newName);
            
            converter.setEntity(entity);
            entityConnector.put(converter);
            
            System.out.println("+++ Reverting the entity to original values.");
            converter.setEntity(copy);
            entityConnector.put(converter);
            
            System.out.println("+++ Deleting created");
            connector.delete();

        } catch (Exception e) {
        	throw new IOException(e);
        }
    }

    public void testSteps() throws IOException {
        try {
            System.out.println("+++ Retrieving Steps:");
            String baseURL = "http://localhost:8080/localindices-harvester/records/";
            
            ResourceConnector<TransformationStepsConverter> connector =
                    new ResourceConnector<TransformationStepsConverter>(
                        new URL(baseURL + "transformationsteps/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            TransformationStepsConverter hc = connector.get();
            for (TransformationStepBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }
            TransformationStep entity = new BasicTransformationStep();
            entity.setName("Test Transformation");
            entity.setDescription("Test Description");

            TransformationStepConverter converter = new TransformationStepConverter();
            converter.setEntity(entity);
            URL resourceURL = connector.postAny(converter);

            System.out.println("+++ Identifier assigned to entity:");
            System.out.println(resourceURL);

            System.out.println("+++ Retrieving the created entity.");
            ResourceConnector<TransformationStepConverter> entityConnector =
                    new ResourceConnector<TransformationStepConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            entity = entityConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved entity:");
            System.out.println("Id: " + entity.getId());
            System.out.println("Name: " + entity.getName());
            
            System.out.println("+++ Updating the storage with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Storage name: " + newName);
            System.out.println("Storage title: " + newTitle);
            
            TransformationStep copy = (TransformationStep) entity.clone();
            entity.setName(newName);
            
            converter.setEntity(entity);
            entityConnector.put(converter);
            
            System.out.println("+++ Reverting the entity to original values.");
            converter.setEntity(copy);
            entityConnector.put(converter);
            
            System.out.println("+++ Deleting created");
            connector.delete();

        } catch (Exception e) {
        	throw new IOException(e);
        }
    }

    
    public void testTransformationStepAssoc() throws IOException {
        try {
            System.out.println("+++ Retrieving Steps:");
            String baseURL = "http://localhost:8080/localindices-harvester/records/";
            
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<TransformationStepAssociationsConverter>(
                        new URL(baseURL + "tsas/"), 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");

            TransformationStepAssociationsConverter hc = connector.get();
            for (TransformationStepAssociationBrief ref : hc.getReferences()) {
                System.out.println(ref.getResourceUri());
            }

            TransformationStep step = new BasicTransformationStep();
            step.setName("Test Step");
            step.setScript("<?xml version=\"1.0\" ?>");
            Transformation transformation = new BasicTransformation();
            transformation.setName("Test Transformation");
            
            
            TransformationStepAssociation entity = new TransformationStepAssociation();
            entity.setPosition(1);
            entity.setStep(step);
            entity.setTransformation(transformation);

            TransformationStepAssociationConverter converter = new TransformationStepAssociationConverter();
            converter.setEntity(entity);
            URL resourceURL = connector.postAny(converter);

            System.out.println("+++ Identifier assigned to entity:");
            System.out.println(resourceURL);

            System.out.println("+++ Retrieving the created entity.");
            ResourceConnector<TransformationStepAssociationConverter> entityConnector =
                    new ResourceConnector<TransformationStepAssociationConverter>(
                        resourceURL, 
                        "com.indexdata.masterkey.localindices.entity" +
                        ":com.indexdata.masterkey.localindices.web.service.converter");
                        
            entity = entityConnector.get().getEntity();
                        
            System.out.println("+++ Retrieved entity:");
            System.out.println("Id: " + entity.getId());
            System.out.println("Step: " + entity.getStep());
            System.out.println("Id: " + entity.getTransformation());
            
            System.out.println("+++ Updating the transformation-step association with new values.");
            
            TransformationStepAssociation copy = (TransformationStepAssociation) entity.clone();
            // TODO add a new step. 
            // Or Change position
            
            converter.setEntity(entity);
            entityConnector.put(converter);
            
            System.out.println("+++ Reverting the entity to original values.");
            converter.setEntity(copy);
            entityConnector.put(converter);
            
            System.out.println("+++ Deleting created");
            connector.delete();

        } catch (Exception e) {
        	throw new IOException(e);
        }
    }

    
}
