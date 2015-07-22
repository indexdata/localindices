package com.indexdata.masterkey.localindices.entity;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.junit.BeforeClass;

import com.indexdata.masterkey.localindices.dao.CommonDAO;
import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.StoragesDAOJPA;
import com.indexdata.masterkey.localindices.dao.bean.TransformationDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.TransformationDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationsDAOJPA;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepDAOWS;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepsDAOJPA;
import com.indexdata.masterkey.localindices.dao.bean.TransformationsDAOJPA;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;
import com.indexdata.utils.persistence.EntityUtilHelper;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class TestDAOs extends TestCase {

	@BeforeClass
    public static void setUpClass() throws Exception {
        // rcarver - setup the jndi context and the datasource
        try {
            // Create initial context
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES, 
                "org.apache.naming");            
            InitialContext ic = new InitialContext();

            ic.createSubcontext("java:");
            ic.createSubcontext("java:/comp");
            ic.createSubcontext("java:/comp/env");
           
            // Construct DataSource
            MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
            ds.setUrl("jdbc:mysql://localhost:3306/localindices");
            ds.setUser("localidxadm");
            ds.setPassword("localidxadmpass");
            ic.bind("java:/comp/env/localindicesDS", ds);
        } catch (NamingException ex) {
            Logger.getLogger(TestDAOs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * @param args the command line arguments
     */
	
	interface EntityTester<Entity> {
		Entity create();
		void print(Entity entity, PrintStream out);
		void modify(Entity entity);
		Long getId(Entity entity);
		Entity clone(Entity entity) throws CloneNotSupportedException;
		void setup();	
		void cleanup();	
	}

	class HarvestableTester implements EntityTester<Harvestable> {
		public Harvestable create() {
			Harvestable entity = new OaiPmhResource();
			entity.setName("test entry");
			entity.setServiceProvider("automatically posted harvestable");
			entity.setTechnicalNotes("relevant description");
			entity.setEnabled(false);
			entity.setCurrentStatus("no status");
			entity.setMaxDbSize(320);
			entity.setScheduleString("0:1:1");
			entity.setLastUpdated(new Date());
			entity.setHarvestImmediately(false);
			return entity;
		}

		public void print(Harvestable entity, PrintStream out) {
			out.println("+++ Retrieved harvestable:");
			out.println("Harvestable id: " + entity.getId());
			out.println("Harvestable name: " + entity.getName());
			out.println("Harvestable title: "
					+ entity.getServiceProvider());
			System.out.println("Harvestable description: "
					+ entity.getTechnicalNotes());
		}
		
		public void modify(Harvestable entity) {
			String newName = "updated resource name";
			String newTitle = "updated title";

			System.out.println("Harvestable name: " + newName);
			System.out.println("Harvestable title: " + newTitle);

			entity.setName(newName);
			entity.setServiceProvider(newTitle);
		}

		@Override
		public Long getId(Harvestable entity) {
			return entity.getId();
		}

		@Override
		public Harvestable clone(Harvestable entity) throws CloneNotSupportedException {
			return (Harvestable) entity.clone();
		}

		@Override
		public void setup() {
		}

		@Override
		public void cleanup() {
		}
	}	
	
	class StorageTestHelper implements EntityTester<Storage> 
	{

		@Override
		public Storage create() {
            Storage entity = new SolrStorageEntity();
            entity.setName("Test " + entity.getClass().getCanonicalName());
            entity.setEnabled(false);
			return entity;
		}

		@Override
		public void print(Storage entity, PrintStream out) {
            System.out.println("+++ Retrieved storage:");
            System.out.println("Storage id: " + entity.getId());
            System.out.println("Storage name: " + entity.getName());
            System.out.println("Storage desc: " + entity.getDescription());
		}

		@Override
		public void modify(Storage entity) {
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Storage name: " + newName);
            System.out.println(" Description: " + newTitle);
            entity.setName(newName);
            entity.setDescription(newTitle);
		}

		@Override
		public Long getId(Storage entity) {
			return entity.getId();
		}

		@Override
		public Storage clone(Storage entity) throws CloneNotSupportedException {
			return (Storage) entity.clone();
		}

		@Override
		public void setup() {
		}

		@Override
		public void cleanup() {
		}
		
	}

	class EntityDAOTester<Entity, EntityBrief> {
		public void testEntity(CommonDAO<Entity, EntityBrief> dao, EntityTester<Entity> tester) throws EntityInUse {
            tester.setup();
			Entity entity = tester.create();
			System.out.println("testEntity " + entity.getClass().getName() + " start");
            List<EntityBrief> list = dao.retrieveBriefs(0, dao.getCount());
            assertTrue(list != null);
			for (EntityBrief ref : list) {
				tester.print(dao.retrieveFromBrief(ref), System.out);
			}

			dao.create(entity);
			System.out.println("Created new entity: " + entity.getClass().getName());
			tester.print(entity, System.out);
			tester.modify(entity);
			dao.update(entity);
			System.out.println("Updated entity with new values.");

			Entity updatedEntity = dao.retrieveById(tester.getId(entity));
			assertTrue("Failed to update entity: " + entity, entity.equals(updatedEntity));
			
			dao.delete(entity);
			System.out.println("Deleted entity.");
			assertTrue("Failed to delete entity: " + entity, dao.retrieveById(tester.getId(entity)) == null);
			tester.cleanup();
			System.out.println("testEntity " + entity.getClass().getName() + " ended.");
		}
	}

	interface DAOFactory {
		HarvestableDAO newHavestableDAO(); 
		StorageDAO newStorageDAO(); 
		TransformationDAO newTransformationDAO(); 
		TransformationStepDAO newTransformationStepDAO(); 
		TransformationStepAssociationDAO newTransformationStepAssociationDAO(); 
	}

	class FakeDAOFactory implements DAOFactory {
		public HarvestableDAO newHavestableDAO() { return new HarvestableDAOFake(); }; 
		public StorageDAO newStorageDAO() { return new StorageDAOFake(); }; 
		public TransformationDAO newTransformationDAO() { return new TransformationDAOFake(); }; 
		public TransformationStepDAO newTransformationStepDAO() { return new TransformationStepDAOFake(); }; 
		public TransformationStepAssociationDAO newTransformationStepAssociationDAO() { return new TransformationStepAssociationDAOFake(); }; 
	}

	class JPADAOFactory implements DAOFactory {
		public HarvestableDAO newHavestableDAO() { return new HarvestablesDAOJPA(); }; 
		public StorageDAO newStorageDAO() { return new StoragesDAOJPA(); }; 
		public TransformationDAO newTransformationDAO() { return new TransformationsDAOJPA(); }; 
		public TransformationStepDAO newTransformationStepDAO() { return new TransformationStepsDAOJPA(); }; 
		public TransformationStepAssociationDAO newTransformationStepAssociationDAO() { return new TransformationStepAssociationsDAOJPA(); }; 
	}

	String baseURL = "http://localhost:8080/harvester/records/";
	class WebServiceDAOFactory implements DAOFactory {
		public HarvestableDAO newHavestableDAO() { return new HarvestableDAOWS(baseURL + "harvestables/"); }; 
		public StorageDAO newStorageDAO() { return new StorageDAOWS(baseURL + "storages/"); }; 
		public TransformationDAO newTransformationDAO() { return new TransformationDAOWS(baseURL + "transformations/"); }; 
		public TransformationStepDAO newTransformationStepDAO() { return new TransformationStepDAOWS(baseURL + "steps/"); }; 
		public TransformationStepAssociationDAO newTransformationStepAssociationDAO() { return new TransformationStepAssociationDAOWS(baseURL + "tsas/"); }; 
	}

	/** 
	 * Fake DAOs doesnt work completely with the tests. The Cascading isnt implemented
	private void testDAOFake() {
		System.out.println("Testing Fake DAOs");
		testDAOs(new FakeDAOFactory());
	}
	 */

	public void testDAOJPA() throws Exception {
		setUpClass();
		EntityUtilHelper.initialize("localindicesPU");
		System.out.println("Testing JPA DAOs");
		JPADAOFactory factory = new JPADAOFactory();
		testDAOs(factory);
		testUITransformationCreateflow(factory);
		testUITransformationEditStep(factory);
	}
	
	public void testDAOWS() throws EntityInUse {
		System.out.println("Testing WebService DAOs");
		WebServiceDAOFactory factory = new WebServiceDAOFactory();
		testDAOs(factory);
		testUITransformationCreateflow(factory);
		testUITransformationEditStep(factory);
	}

	
	public void testDAOs(DAOFactory factory) throws EntityInUse {
		HarvestableDAO dao = factory.newHavestableDAO(); 
		EntityDAOTester<Harvestable, HarvestableBrief> test = new EntityDAOTester<Harvestable, HarvestableBrief>();
		testHarvestables(dao);
		// Template way
		test.testEntity(dao, new HarvestableTester());

		StorageDAO storageDao = factory.newStorageDAO(); 
		EntityDAOTester<Storage, StorageBrief> storageTest = new EntityDAOTester<Storage, StorageBrief>();
		storageTest.testEntity(storageDao , new StorageTestHelper());

		TransformationDAO transformationDao = factory.newTransformationDAO();
		EntityDAOTester<Transformation, TransformationBrief> transformationTest = new EntityDAOTester<Transformation, TransformationBrief>();
		transformationTest.testEntity(transformationDao , new TransformationTestHelper());

		TransformationStepDAO stepDao = factory.newTransformationStepDAO();
		EntityDAOTester<TransformationStep, TransformationStepBrief> transformationStepTest = new EntityDAOTester<TransformationStep, TransformationStepBrief>();
		transformationStepTest.testEntity(stepDao , new TransformationStepTestHelper());

		TransformationStepAssociationDAO tsaDao = factory.newTransformationStepAssociationDAO(); 
		EntityDAOTester<TransformationStepAssociation, TransformationStepAssociationBrief> tsaTest = new EntityDAOTester<TransformationStepAssociation, TransformationStepAssociationBrief>();
		tsaTest.testEntity(tsaDao , new TransformationStepAssociationTestHelper(transformationDao, stepDao));
	}
	
	private void testUITransformationCreateflow(DAOFactory factory) throws EntityInUse {
		TransformationDAO transformationDao = factory.newTransformationDAO(); 
		
		TransformationTestHelper transformationHelper = new TransformationTestHelper();
		TransformationStepTestHelper stepHelper = new TransformationStepTestHelper();
		//TransformationStepAssociationTestHelper tsaHelper = new TransformationStepAssociationTestHelper(transformationDao, stepDao);
		transformationHelper.setup();
		Transformation transformation = transformationHelper.create();
		transformationDao.create(transformation);
		TransformationStep step = stepHelper.create();
		//stepDao.create(step);
		TransformationStepAssociation tsa = new TransformationStepAssociation();
		tsa.setPosition(1);
		tsa.setStep(step);
		tsa.setTransformation(transformation);
		transformation.addStepAssociation(tsa);
		transformationDao.update(transformation);
		
		TransformationStepDAO stepDao = factory.newTransformationStepDAO();
		TransformationStepAssociationDAO tsaDao = factory.newTransformationStepAssociationDAO();
		// Validate
		
		// Clean up
		tsaDao.delete(tsa);
		stepDao.delete(step);
		transformationDao.delete(transformation);
		// Validate clean up
	}

	private void testUITransformationEditStep(DAOFactory factory) throws EntityInUse {
		TransformationDAO transformationDao = factory.newTransformationDAO(); 
		TransformationTestHelper transformationHelper = new TransformationTestHelper();
		TransformationStepTestHelper stepHelper = new TransformationStepTestHelper();
		TransformationStepAssociationDAO tsaDao = factory.newTransformationStepAssociationDAO();
		TransformationStepDAO stepDao = factory.newTransformationStepDAO();
		//TransformationStepAssociationTestHelper tsaHelper = new TransformationStepAssociationTestHelper(transformationDao, stepDao);

		transformationHelper.setup();
		Transformation transformation = transformationHelper.create();
		transformationDao.create(transformation);

		TransformationStep step = stepHelper.create();
		//stepDao.create(step);
		
		TransformationStepAssociation tsa = new TransformationStepAssociation();
		tsa.setPosition(1);
		tsa.setStep(step);
		tsa.setTransformation(transformation);
		transformation.addStepAssociation(tsa);
		tsaDao.create(tsa);
		transformationDao.update(transformation);

		step.setScript("<new-script/>");
		tsaDao.update(tsa);
		
		// Validate
		
		// Clean up
		tsaDao.delete(tsa);
		stepDao.delete(step);
		transformationDao.delete(transformation);
		// Validate clean up
	}

	
	
	public void testHarvestables(HarvestableDAO dao) throws EntityInUse {
        	List<Harvestable> list = dao.retrieve(0, dao.getCount());
            
            for (Harvestable ref : list) {
                System.out.println(ref.getName());
            }
            
            System.out.println("+++ Creating new harvestable.");
            HarvestableTester testHelper  = new HarvestableTester();
            Harvestable entity = testHelper.create();
            
            dao.create(entity);
            System.out.println("+++ Retrieved harvestable:");
            System.out.println("Harvestable id: " + entity.getId());
            System.out.println("Harvestable name: " + entity.getName());
            System.out.println("Harvestable title: " + entity.getServiceProvider());
            System.out.println("Harvestable description: " + entity.getTechnicalNotes());

            System.out.println("+++ Updating the harvestable with new values.");
            String newName = "updated resource name";
            String newTitle = "updated title";
            
            System.out.println("Harvestable name: " + newName);
            System.out.println("Harvestable title: " + newTitle);
            
            entity.setName(newName);
            entity.setServiceProvider(newTitle);
            dao.update(entity);
            Harvestable updatedEntity = dao.retrieveById(entity.getId());
            assertTrue("Failed to update entity: " + entity, entity.equals(updatedEntity));
            //assertFalse((entity.equals(copy)));
            dao.delete(entity);
            assertTrue("Failed to update entity: " + entity, dao.retrieveById(entity.getId())==null);
    }

    
    public void testStorages(StorageDAO dao) throws IOException {
        try {
        	StorageTestHelper helper = new StorageTestHelper();
        	
        	List<Storage> list = dao.retrieve(0, dao.getCount());
        	
            for (Storage ref : list) {
                System.out.println(ref.getId());
            }
            
            System.out.println("+++ Creating new instance ");

            Storage entity = helper.create();
            dao.create(entity);
                        
            Storage fetched = dao.retrieveById(entity.getId());
            
            helper.print(fetched, System.out);
            // TODO assert: entity == fetched
            
            Storage storageCopy = (Storage) entity.clone();
            System.out.println("+++ Updating the storage with new values.");
            helper.modify(entity);
            dao.update(entity);
            // TODO assert updates 

            dao.update(storageCopy);
            
            System.out.println("+++ Deleting created storage.");
            dao.delete(entity);

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void testTransformations(TransformationDAO dao) throws IOException, CloneNotSupportedException {
		List<TransformationBrief> list = dao.retrieveBriefs(0, dao.getCount());

		for (TransformationBrief ref : list) {
			System.out.println(ref.getResourceUri());
		}

		TransformationTestHelper helper = new TransformationTestHelper();
		helper.setup();
		Transformation entity = helper.create();
		dao.create(entity);
		Transformation previous = entity;
		entity = dao.retrieveById(entity.getId());
		System.out.println("+++ Identifier assigned to entity:"
				+ helper.getId(entity));
		previous.equals(entity);
		helper.print(entity, System.out);
		helper.modify(entity);
		Transformation copy = (Transformation) entity.clone();

		dao.update(entity);

		System.out.println("+++ Reverting the entity to original values.");
		dao.update(copy);

		System.out.println("+++ Deleting created");
		dao.delete(entity);
    }

    public void testSteps(TransformationStepDAO dao) throws IOException {
        try {

        	List<TransformationStepBrief> list = dao.retrieveBriefs(0, dao.getCount());

            for (TransformationStepBrief ref : list) {
                System.out.println(ref.getResourceUri());
            }

            TransformationStep entity = new XmlTransformationStep();
            entity.setName("Test Transformation");
            entity.setDescription("Test Description");

            dao.create(entity);
            
            TransformationStep previous = entity;
            entity = dao.retrieveById(previous.getId());
            // TODO assert entity equals previous
            
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
            
            dao.update(entity);
            
            System.out.println("+++ Reverting the entity to original values.");
            dao.update(copy);
            
            System.out.println("+++ Deleting created");
            dao.delete(entity);

        } catch (Exception e) {
        	throw new IOException(e);
        }
    }

    
    public void testTransformationStepAssoc(TransformationStepAssociationDAO dao, 
    										TransformationDAO transformationDAO,
    										TransformationStepDAO stepDAO) throws IOException {
    	TransformationStepAssociationTestHelper helper = new TransformationStepAssociationTestHelper(transformationDAO, stepDAO);
    	helper.setup();
        try {
        	List<TransformationStepAssociation> list = dao.retrieve(0, dao.getCount());
        	for (TransformationStepAssociation assoc : list) {
        		helper.print(assoc, System.out);
         	}
            
            TransformationStepAssociation entity = helper.create();

            dao.create(entity);
            TransformationStepAssociation previous = entity;
            System.out.println("+++ Retrieving the created entity.");
            entity = dao.retrieveById(previous.getId());
            assertTrue("Failed to retrieve new entity: " + entity, entity.equals(previous));
            helper.print(entity, System.out);
            
            System.out.println("+++ Updating the transformation-step association with new values.");
            
            helper.modify(entity);
            dao.update(entity);
            
            dao.delete(entity);
            assertTrue("Failed to delete entity: " + entity, dao.retrieveById(previous.getId()) == null);

        } catch (Exception e) {
        	throw new IOException(e);
        }
        helper.cleanup();
    }

    
}
