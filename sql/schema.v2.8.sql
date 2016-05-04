-- MySQL dump 10.13  Distrib 5.6.23, for osx10.10 (x86_64)
--
-- Host: localhost    Database: localindices
-- ------------------------------------------------------
-- Server version	5.6.23

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
  `DESCRIPTION` mediumtext,
  `INITIALLYHARVESTED` datetime DEFAULT NULL,
  `TECHNICALNOTES` mediumtext,
  `LASTHARVESTSTARTED` datetime DEFAULT NULL,
  `SCHEDULESTRING` varchar(255) DEFAULT NULL,
  `LASTHARVESTFINISHED` datetime DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `CURRENTSTATUS` varchar(255) DEFAULT NULL,
  `CONTACTNOTES` mediumtext,
  `AMOUNTHARVESTED` int(11) DEFAULT NULL,
  `SERVICEPROVIDER` varchar(255) DEFAULT NULL,
  `MESSAGE` mediumtext,
  `MAXDBSIZE` int(11) DEFAULT NULL,
  `HARVESTIMMEDIATELY` tinyint(1) NOT NULL DEFAULT '0',
  `FILETYPEMASKS` varchar(255) DEFAULT NULL,
  `URIMASKS` varchar(255) DEFAULT NULL,
  `STARTURLS` mediumtext,
  `RECURSIONDEPTH` int(11) DEFAULT NULL,
  `EXPECTEDSCHEMA` varchar(255) DEFAULT NULL,
  `URL` mediumtext,
  `NORMALIZATIONFILTER` varchar(255) DEFAULT NULL,
  `SCHEMAURI` varchar(255) DEFAULT NULL,
  `OAISETNAME` varchar(255) DEFAULT NULL,
  `FROMDATE` datetime DEFAULT NULL,
  `UNTILDATE` datetime DEFAULT NULL,
  `METADATAPREFIX` varchar(255) DEFAULT NULL,
  `DATEFORMAT` varchar(255) DEFAULT NULL,
  `RESUMPTIONTOKEN` varchar(255) DEFAULT NULL,
  `STORAGE_ID` bigint(20) DEFAULT NULL,
  `TRANSFORMATION_ID` bigint(20) DEFAULT NULL,
  `OVERWRITE` tinyint(1) DEFAULT NULL,
  `SPLITAT` varchar(255) DEFAULT NULL,
  `SPLITSIZE` varchar(255) DEFAULT NULL,
  `OUTPUTSCHEMA` mediumtext,
  `ENCODING` varchar(255) DEFAULT NULL,
  `CLIENTCLASS` varchar(255) DEFAULT NULL,
  `USERNAME` varchar(255) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `PROXY` varchar(255) DEFAULT NULL,
  `SCRIPT` longtext,
  `ISPERSISTENCE` tinyint(1) DEFAULT NULL,
  `INITDATA` mediumtext,
  `SLEEP` int(11) DEFAULT NULL,
  `CONNECTORURL` mediumtext,
  `ALLOWERRORS` tinyint(4) DEFAULT NULL,
  `ALLOWCONDREQ` tinyint(4) DEFAULT NULL,
  `CLEARRTONERROR` tinyint(1) NOT NULL DEFAULT '0',
  `KEEPPARTIAL` tinyint(1) NOT NULL DEFAULT '1',
  `TIMEOUT` smallint(6) NOT NULL DEFAULT '300',
  `RETRYCOUNT` tinyint(3) NOT NULL DEFAULT '2',
  `RETRYWAIT` smallint(4) NOT NULL DEFAULT '60',
  `LOGLEVEL` varchar(30) DEFAULT NULL,
  `MAILLEVEL` varchar(30) DEFAULT NULL,
  `MAILADDRESS` varchar(1000) DEFAULT NULL,
  `RECORDLIMIT` bigint(20) DEFAULT NULL,
  `DISKRUN` tinyint(1) NOT NULL DEFAULT '0',
  `CACHEENABLED` tinyint(1) NOT NULL DEFAULT '0',
  `ENGINEPARAMETERS` varchar(4096) DEFAULT NULL,
  `CONNECTORENGINEURLSETTING_ID` bigint(20) DEFAULT NULL,
  `CONNECTORREPOURLSETTING_ID` bigint(20) DEFAULT NULL,
  `OPENACCESS` tinyint(1) NOT NULL DEFAULT '0',
  `ORIGINALURI` varchar(1024) DEFAULT NULL,
  `JSON` text,
  `LAXPARSING` tinyint(1) NOT NULL DEFAULT '0',
  `PASSIVEMODE` tinyint(1) NOT NULL DEFAULT '0',
  `RECURSIONLEVELS` tinyint(2) NOT NULL DEFAULT '0',
  `CSVCONFIGURATION` varchar(1024) DEFAULT NULL,
  `EXCLUDEFILEPATTERN` varchar(1024) DEFAULT NULL,
  `INCLUDEFILEPATTERN` varchar(1024) DEFAULT NULL,
  `STORAGEBATCHLIMIT` smallint(4) DEFAULT NULL,
  `MANAGEDBY` varchar(1024) DEFAULT NULL,
  `USEDBY` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_HARVESTABLE_STORAGE_ID` (`STORAGE_ID`),
  CONSTRAINT `FK_HARVESTABLE_STORAGE_ID` FOREIGN KEY (`STORAGE_ID`) REFERENCES `storage` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

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

LOCK TABLES `SEQUENCE` WRITE;
/*!40000 ALTER TABLE `SEQUENCE` DISABLE KEYS */;
INSERT INTO `SEQUENCE` VALUES ('SEQ_GEN',1);
/*!40000 ALTER TABLE `SEQUENCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SETTING`
--

DROP TABLE IF EXISTS `SETTING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SETTING` (
  `ID` bigint(20) NOT NULL,
  `NAME` varchar(4096) NOT NULL,
  `LABEL` varchar(4096) NOT NULL,
  `VALUE` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STEP`
--

DROP TABLE IF EXISTS `STEP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STEP` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `DESCRIPTION` varchar(4096) DEFAULT NULL,
  `ENABLED` tinyint(1) DEFAULT '0',
  `NAME` varchar(255) DEFAULT NULL,
  `SCRIPT` text,
  `TYPE` varchar(255) DEFAULT NULL,
  `OUTPUTFORMAT` varchar(255) DEFAULT NULL,
  `INPUTFORMAT` varchar(255) DEFAULT NULL,
  `CUSTOMCLASS` varchar(255) DEFAULT NULL,
  `TESTDATA` text,
  `TESTOUTPUT` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `STORAGE`
--

DROP TABLE IF EXISTS `STORAGE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STORAGE` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `ENABLED` tinyint(1) DEFAULT '0',
  `CURRENTSTATUS` varchar(4096) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `MESSAGE` varchar(4096) DEFAULT NULL,
  `TRANSFORMATION` varchar(100) DEFAULT NULL,
  `DESCRIPTION` varchar(4096) DEFAULT NULL,
  `URL` varchar(255) DEFAULT NULL,
  `CUSTOMCLASS` varchar(1024) DEFAULT NULL,
  `LOGLEVEL` varchar(30) DEFAULT NULL,
  `MAILLEVEL` varchar(30) DEFAULT NULL,
  `MAILADDRESS` varchar(1000) DEFAULT NULL,
  `BULKSIZE` bigint(20) DEFAULT '1000',
  `TIMEOUT` smallint(4) DEFAULT NULL,
  `RETRYCOUNT` tinyint(3) DEFAULT NULL,
  `RETRYWAIT` smallint(4) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `PARALLEL` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TRANSFORMATION_STEP`
--

DROP TABLE IF EXISTS `TRANSFORMATION_STEP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TRANSFORMATION_STEP` (
  `ID` bigint(20) NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `POSITION` int(11) DEFAULT NULL,
  `TRANSFORMATION_ID` bigint(20) NOT NULL,
  `STEP_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_TRANSFORMATION_STEP_STEP_ID` (`STEP_ID`),
  KEY `TRANSFORMATION_STEP_TRANSFORMATION_ID` (`TRANSFORMATION_ID`),
  CONSTRAINT `FK_TRANSFORMATION_STEP_STEP_ID` FOREIGN KEY (`STEP_ID`) REFERENCES `step` (`ID`),
  CONSTRAINT `FK_TRANSFORMATION_STEP_TRANSFORMATION_ID` FOREIGN KEY (`TRANSFORMATION_ID`) REFERENCES `transformation` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-06-22 13:30:28
