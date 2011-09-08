-- MySQL dump 10.13  Distrib 5.1.49, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: localindices
-- ------------------------------------------------------
-- Server version	5.1.49-3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `HARVESTABLE`
--

DROP TABLE IF EXISTS `HARVESTABLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `HARVESTABLE` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `ENABLED` tinyint(1) DEFAULT '0',
  `LASTUPDATED` datetime DEFAULT NULL,
  `DESCRIPTION` varchar(4096) DEFAULT NULL,
  `INITIALLYHARVESTED` datetime DEFAULT NULL,
  `TECHNICALNOTES` varchar(4096) DEFAULT NULL,
  `LASTHARVESTSTARTED` datetime DEFAULT NULL,
  `SCHEDULESTRING` varchar(255) DEFAULT NULL,
  `LASTHARVESTFINISHED` datetime DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `CURRENTSTATUS` varchar(255) DEFAULT NULL,
  `CONTACTNOTES` varchar(4096) DEFAULT NULL,
  `AMOUNTHARVESTED` int(11) DEFAULT NULL,
  `SERVICEPROVIDER` varchar(255) DEFAULT NULL,
  `MESSAGE` varchar(4096) DEFAULT NULL,
  `MAXDBSIZE` int(11) DEFAULT NULL,
  `HARVESTIMMEDIATELY` tinyint(1) NOT NULL DEFAULT '0',
  `FILETYPEMASKS` varchar(255) DEFAULT NULL,
  `URIMASKS` varchar(255) DEFAULT NULL,
  `STARTURLS` varchar(16384) DEFAULT NULL,
  `RECURSIONDEPTH` int(11) DEFAULT NULL,
  `EXPECTEDSCHEMA` varchar(255) DEFAULT NULL,
  `URL` varchar(16384) DEFAULT NULL,
  `NORMALIZATIONFILTER` varchar(255) DEFAULT NULL,
  `SCHEMAURI` varchar(255) DEFAULT NULL,
  `OAISETNAME` varchar(255) DEFAULT NULL,
  `FROMDATE` date DEFAULT NULL,
  `UNTILDATE` date DEFAULT NULL,
  `METADATAPREFIX` varchar(255) DEFAULT NULL,
  `DATEFORMAT` varchar(255) DEFAULT NULL,
  `RESUMPTIONTOKEN` varchar(255) DEFAULT NULL,
  `STORAGE_ID` bigint(20) DEFAULT NULL,
  `TRANSFORMATION_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_HARVESTABLE_STORAGE_ID` (`STORAGE_ID`),
  CONSTRAINT `FK_HARVESTABLE_STORAGE_ID` FOREIGN KEY (`STORAGE_ID`) REFERENCES `STORAGE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `HARVESTABLE`
--

LOCK TABLES `HARVESTABLE` WRITE;
/*!40000 ALTER TABLE `HARVESTABLE` DISABLE KEYS */;
INSERT INTO `HARVESTABLE` VALUES (52,'OaiPmhResource',1,'2011-08-05 12:43:14','','2011-05-12 16:00:34','','2011-08-30 16:53:08','53 16 * * *','2011-08-29 16:53:13','RUG NL','ERROR','',NULL,'','OAI error noRecordsMatch: No matching records in this repository',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://ir.ub.rug.nl/oai/',NULL,NULL,NULL,'2011-08-28','2011-08-29','oai_dc','yyyy-MM-dd',NULL,2,10),(53,'OaiPmhResource',0,'2011-06-29 17:34:55','',NULL,'',NULL,'0 0 * * *',NULL,'Test','NEW','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'','',NULL,'',NULL,NULL,'oai_dc','yyyy-MM-dd',NULL,2,10),(102,'OaiPmhResource',1,'2011-08-08 13:35:17','First feed from Thomas Dowling','2011-08-05 12:36:26','','2011-08-30 10:00:09','0 10 * * *','2011-08-29 15:28:52','Ohiolink ','RUNNING','',NULL,'Ohiolink',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://olc3.ohiolink.edu/~tdowling/oai/request.cgi','414828:20110829:20110829:912501',NULL,NULL,'2011-08-29','2011-08-29','oai_dc','yyyy-MM-dd',NULL,2,10),(103,'OaiPmhResource',1,'2011-08-05 12:42:06','','2011-05-18 13:31:38','','2011-08-31 06:00:04','0 6 * * *','2011-08-30 06:00:18','Alexandria','ERROR','',NULL,'Alexandria - University of St.Gallen (Switzerland)','OAI error noRecordsMatch: The combination of the given values results in an empty list.',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.alexandria.unisg.ch/EXPORT/OAI/server.oai',NULL,NULL,NULL,'2011-08-30','2011-08-30','oai_dc','yyyy-MM-dd',NULL,2,10),(152,'OaiPmhResource',1,'2011-08-05 12:43:24','','2011-05-18 13:38:59','','2011-08-31 15:00:01','0 15 * * *','2011-08-31 15:00:11','Zurich Open Repository and Archive','WAITING','',NULL,'Zurich Open Repository and Archive',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.zora.uzh.ch/cgi/oai2',NULL,NULL,NULL,'2011-08-31',NULL,'oai_dc','yyyy-MM-dd',NULL,2,10),(156,'OaiPmhResource',1,'2011-06-12 14:43:52','','2011-05-18 14:27:12','','2011-08-31 12:00:06','0 12 * * *','2011-08-31 12:00:16','Discovery','WAITING','',NULL,'Dundee AC UK',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://discovery.dundee.ac.uk/oai/request',NULL,NULL,NULL,'2011-08-31',NULL,'oai_dc','yyyy-MM-dd',NULL,2,10),(202,'OaiPmhResource',1,'2011-08-05 12:42:32','http://www.diva-portal.org/dice/oai?verb=ListRecords&metadataPrefix=marc21','2011-05-22 17:16:50','','2011-08-31 13:00:08','0 13 * * *','2011-08-31 13:00:18','Diva-portal (marc21)','WAITING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.diva-portal.org/dice/oai',NULL,NULL,NULL,'2011-08-31',NULL,'marc21','yyyy-MM-dd',NULL,2,20),(252,'OaiPmhResource',1,'2011-06-12 14:45:53','','2011-05-22 21:34:17','','2011-08-30 18:00:08','0 18 * * *','2011-08-08 18:00:15','acceda.ulpgc.es','ERROR','',NULL,'','Http error \'500 Error Interno del Servidor\' when contacting http://acceda.ulpgc.es/oai/request?verb=ListRecords&from=2011-08-08&until=2011-08-29&metadataPrefix=oai_dc',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://acceda.ulpgc.es/oai/request',NULL,NULL,NULL,'2011-08-08','2011-08-29','oai_dc','yyyy-MM-dd',NULL,2,10),(253,'OaiPmhResource',1,'2011-08-05 12:42:20','','2011-05-22 21:40:47','','2011-08-31 09:00:06','0 9 * * *','2011-06-12 14:43:15','Annals of Genealogical Research','ERROR','',NULL,'','OAI error noRecordsMatch: No matching records in this repository',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.genlit.org/agr/oai/','',NULL,NULL,'2011-06-12','2011-08-30','oai_dc','yyyy-MM-dd',NULL,3,10),(254,'OaiPmhResource',1,'2011-06-12 14:43:15','','2011-05-22 21:53:38','','2011-08-31 10:00:05','0 10 * * *','2011-08-30 10:00:19','Aston University Research Archive','ERROR','',NULL,'','OAI error noRecordsMatch: No items match. None. None at all. Not even deleted ones.',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://eprints.aston.ac.uk/cgi/oai2',NULL,NULL,NULL,'2011-08-28','2011-08-30','oai_dc','yyyy-MM-dd',NULL,2,10),(255,'OaiPmhResource',1,'2011-08-08 13:37:14','','2011-05-22 22:01:29','','2011-08-31 11:00:06','0 11 * * *','2011-06-12 14:43:46','Internet Archive','ERROR','',NULL,'','Resetting to invalid mark',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.archive.org/services/oai.php','',NULL,'collection:texts',NULL,'2011-08-30','oai_dc','yyyy-MM-dd',NULL,2,10),(303,'OaiPmhResource',1,'2011-06-12 13:48:23','','2011-05-23 10:41:06','','2011-08-31 14:00:00','0 14 * * *','2011-08-31 14:00:10','Universität Potsdam','WAITING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://opus.kobv.de/ubp/oai2/oai2.php',NULL,NULL,NULL,'2011-08-31',NULL,'oai_dc','yyyy-MM-dd',NULL,2,10);
/*!40000 ALTER TABLE `HARVESTABLE` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-08-31 15:14:22
