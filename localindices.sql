-- MySQL dump 10.13  Distrib 5.5.12, for osx10.6 (i386)
--
-- Host: localhost    Database: localindices
-- ------------------------------------------------------
-- Server version	5.5.12

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
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `HARVESTABLE`
--

LOCK TABLES `HARVESTABLE` WRITE;
/*!40000 ALTER TABLE `HARVESTABLE` DISABLE KEYS */;
INSERT INTO `HARVESTABLE` VALUES (52,'OaiPmhResource',1,'2011-05-23 09:25:56','','2011-05-12 16:00:34','','2011-05-24 16:53:05','53 16 * * *','2011-05-23 09:35:32','RUG NL','RUNNING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://ir.ub.rug.nl/oai/',NULL,NULL,NULL,'2011-05-23','2011-05-23','oai_dc','yyyy-MM-dd',NULL),(103,'OaiPmhResource',1,'2011-05-31 14:43:11','','2011-05-18 13:31:38','','2011-05-26 10:23:32','0 6 * * *','2011-05-26 10:23:53','Alexandria','NEW','',NULL,'Alexandria - University of St.Gallen (Switzerland)',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.alexandria.unisg.ch/EXPORT/OAI/server.oai','',NULL,'','2011-05-26',NULL,'oai_dc','yyyy-MM-dd',NULL),(152,'OaiPmhResource',1,'2011-05-26 12:56:36','','2011-05-18 13:38:59','','2011-05-31 15:00:05','0 15 * * *','2011-05-31 15:00:15','Zurich Open Repository and Archive','WAITING','',NULL,'Zurich Open Repository and Archive',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.zora.uzh.ch/cgi/oai2',NULL,NULL,NULL,'2011-05-31',NULL,'oai_dc','yyyy-MM-dd',NULL),(156,'OaiPmhResource',1,'2011-05-31 13:48:42','','2011-05-18 14:27:12','','2011-05-23 09:23:58','0 12 * * *','2011-05-23 09:26:39','Discovery','NEW','',NULL,'Dundee AC UK',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://discovery.dundee.ac.uk/oai/request','',NULL,'','2011-05-23',NULL,'oai_dc','yyyy-MM-dd',NULL),(202,'OaiPmhResource',1,'2011-05-26 12:56:02','http://www.diva-portal.org/dice/oai?verb=ListRecords&metadataPrefix=marc21','2011-05-22 17:16:50','','2011-05-31 13:00:07','0 13 * * *','2011-05-31 13:00:17','Diva-portal (marc21)','WAITING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.diva-portal.org/dice/oai',NULL,NULL,NULL,'2011-05-31',NULL,'marc21','yyyy-MM-dd',NULL),(252,'OaiPmhResource',1,'2011-05-26 12:56:51','','2011-05-22 21:34:17','','2011-05-30 18:00:09','0 18 * * *','2011-05-30 18:00:19','acceda.ulpgc.es','WAITING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://acceda.ulpgc.es/oai/request',NULL,NULL,NULL,'2011-05-30',NULL,'oai_dc','yyyy-MM-dd',NULL),(253,'OaiPmhResource',1,'2011-05-31 14:43:05','','2011-05-22 21:40:47','','2011-05-29 09:00:06','0 9 * * *','2011-05-23 09:25:18','Annals of Genealogical Research','NEW','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://www.genlit.org/agr/oai/','',NULL,'','2011-05-23','2011-05-28','oai_dc','yyyy-MM-dd',NULL),(254,'OaiPmhResource',1,'2011-05-26 12:55:26','','2011-05-22 21:53:38','','2011-05-31 10:00:02','0 10 * * *','2011-05-31 10:00:12','Aston University Research Archive','WAITING','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://eprints.aston.ac.uk/cgi/oai2',NULL,NULL,NULL,'2011-05-31',NULL,'oai_dc','yyyy-MM-dd',NULL),(255,'OaiPmhResource',1,'2011-05-26 12:55:36','','2011-05-22 22:01:29','','2011-05-30 11:00:08','0 11 * * *','2011-05-23 09:25:48','Athenea Digital','ERROR','',NULL,'','OAI error noRecordsMatch: No matching records in this repository',NULL,0,NULL,NULL,NULL,NULL,NULL,'http://psicologiasocial.uab.es/athenea/index.php/atheneaDigital/oai','',NULL,NULL,'2011-05-23','2011-05-29','oai_dc','yyyy-MM-dd',NULL),(303,'OaiPmhResource',1,'2011-05-31 14:44:46','','2011-05-23 10:41:06','','2011-05-31 14:00:06','0 14 * * *','2011-05-29 14:00:13','Universität Potsdam','NEW','',NULL,'',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,'http://opus.kobv.de/ubp/oai2/oai2.php','',NULL,'','2011-05-29','2011-05-30','oai_dc','yyyy-MM-dd',NULL);
/*!40000 ALTER TABLE `HARVESTABLE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SEQUENCE`
--

DROP TABLE IF EXISTS `SEQUENCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SEQUENCE` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SEQUENCE`
--

LOCK TABLES `SEQUENCE` WRITE;
/*!40000 ALTER TABLE `SEQUENCE` DISABLE KEYS */;
INSERT INTO `SEQUENCE` VALUES ('SEQ_GEN',351);
/*!40000 ALTER TABLE `SEQUENCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STORAGE`
--

DROP TABLE IF EXISTS `STORAGE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STORAGE` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `DESCRIPTION` varchar(4096) DEFAULT NULL,
  `ENABLED` tinyint(1) DEFAULT '0',
  `NAME` varchar(255) DEFAULT NULL,
  `CURRENTSTATUS` varchar(4096) DEFAULT NULL,
  `MESSAGE` varchar(4096) DEFAULT NULL,
  `URL` varchar(255) DEFAULT NULL,
  `TRANSFORMATION` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STORAGE`
--

LOCK TABLES `STORAGE` WRITE;
/*!40000 ALTER TABLE `STORAGE` DISABLE KEYS */;
/*!40000 ALTER TABLE `STORAGE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TRANSFORMATION`
--

DROP TABLE IF EXISTS `TRANSFORMATION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TRANSFORMATION` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `DESCRIPTION` varchar(4096) DEFAULT NULL,
  `ENABLED` tinyint(1) DEFAULT '0',
  `NAME` varchar(255) DEFAULT NULL,
  `CURRENTSTATUS` varchar(4096) DEFAULT NULL,
  `MESSAGE` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TRANSFORMATION`
--

LOCK TABLES `TRANSFORMATION` WRITE;
/*!40000 ALTER TABLE `TRANSFORMATION` DISABLE KEYS */;
/*!40000 ALTER TABLE `TRANSFORMATION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TRANSFORMATION_TRANSFORMATIONSTEP`
--

DROP TABLE IF EXISTS `TRANSFORMATION_TRANSFORMATIONSTEP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TRANSFORMATION_TRANSFORMATIONSTEP` (
  `Transformation_ID` bigint(20) NOT NULL,
  `steps_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`Transformation_ID`,`steps_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TRANSFORMATION_TRANSFORMATIONSTEP`
--

LOCK TABLES `TRANSFORMATION_TRANSFORMATIONSTEP` WRITE;
/*!40000 ALTER TABLE `TRANSFORMATION_TRANSFORMATIONSTEP` DISABLE KEYS */;
/*!40000 ALTER TABLE `TRANSFORMATION_TRANSFORMATIONSTEP` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-05-31 15:53:26
