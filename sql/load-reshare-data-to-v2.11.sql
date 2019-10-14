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

LOCK TABLES `STORAGE` WRITE;
/*!40000 ALTER TABLE `STORAGE` DISABLE KEYS */;
INSERT INTO `STORAGE` VALUES (204,'InventoryStorageEntity',1,'TODO','FOLIO @ localhost',NULL,NULL,'FOLIO','http://10.0.2.2:9130',NULL,NULL,NULL,NULL,1000,60,2,60);
INSERT INTO `STORAGE` VALUES (205,'InventoryStorageEntity',1,'TODO','Reshare demo FOLIO server',NULL,NULL,'FOLIO','http://shared-index.reshare-dev.indexdata.com:9130',NULL,NULL,NULL,NULL,1000,60,2,60);
/* INSERT INTO `STORAGE` VALUES (205,'InventoryStorageEntity',1,'TODO','FOLIO Inventory Match @ localhost',NULL,NULL,'Inventory Match at localhost','http://10.0.2.2:9130/instance-storage-match/instances',NULL,NULL,NULL,NULL,1000,60,2,60); */
/*!40000 ALTER TABLE `STORAGE` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `HARVESTABLE` WRITE;
/*!40000 ALTER TABLE `HARVESTABLE` DISABLE KEYS */;
INSERT INTO `HARVESTABLE` VALUES (20001, 'XmlBulkResource',1,'2014-09-24 08:33:27','','2011-12-28 21:22:32','','2015-06-10 10:10:00','10 10 10 6 *','2015-06-10 10:32:11','Reshare: Test data'                          ,'OK','',NULL, 'Index Data',NULL,NULL,0,NULL,NULL,NULL,NULL,'','http://localhost:8080/test/oai2instance.xml'   ,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,204,2002,1,'2','1000','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,0,1,60,2,60,'DEBUG','WARN','',NULL,0,0,NULL,1,2,1,NULL,'{\r\n\ \"folioAuthPath\": \"/bl-users/login\",\r\n \"folioTenant\": \"diku\",\r\n \"folioUsername\": \"diku_admin\",\r\n \"folioPassword\": \"admin\",\r\n \"instanceStoragePath\": \"/instance-storage/instances\",\r\n \"holdingsStoragePath\": \"/holdings-storage/holdings\",\r\n \"itemStoragePath\": \"/item-storage/items\"\r\n}',0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `HARVESTABLE` VALUES (20002, 'XmlBulkResource',1,'2014-09-24 08:33:27','','2011-12-28 21:22:32','','2015-06-10 10:10:00','10 10 10 6 *','2015-06-10 10:32:11','Reshare: Millersville OAI-PMH sample'        ,'OK','',NULL, 'Index Data',NULL,NULL,0,NULL,NULL,NULL,NULL,'','http://localhost:8080/test/alma_millersville_oai_sample.xml',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,204,2002,1,'2','1000','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,0,1,60,2,60,'DEBUG','WARN','',NULL,0,0,NULL,1,2,1,NULL,'{\r\n\ \"folioAuthPath\": \"/bl-users/login\",\r\n \"folioTenant\": \"diku\",\r\n \"folioUsername\": \"diku_admin\",\r\n \"folioPassword\": \"admin\",\r\n \"instanceStoragePath\": \"/instance-storage/instances\",\r\n \"holdingsStoragePath\": \"/holdings-storage/holdings\",\r\n \"itemStoragePath\": \"/item-storage/items\"\r\n}',0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `HARVESTABLE` VALUES (20003, 'XmlBulkResource',1,'2014-09-24 08:33:27','','2011-12-28 21:22:32','','2015-06-10 10:10:00','10 10 10 6 *','2015-06-10 10:32:11','Demo East Town OAI-PMH'        ,'OK','',NULL, 'Index Data',NULL,NULL,0,NULL,NULL,NULL,NULL,'','http://localhost:8080/test/alma_oai_pmh_east_town.xml',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,204,2002,1,'2','1000','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,0,1,60,2,60,'DEBUG','WARN','',NULL,0,0,NULL,1,2,1,NULL,'{\r\n\ \"folioAuthPath\": \"/bl-users/login\",\r\n \"folioTenant\": \"diku\",\r\n \"folioUsername\": \"diku_admin\",\r\n \"folioPassword\": \"admin\",\r\n \"instanceStoragePath\": \"/instance-storage-match/instances\",\r\n \"holdingsStoragePath\": \"/holdings-storage/holdings\",\r\n \"itemStoragePath\": \"/item-storage/items\"\r\n}',0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `HARVESTABLE` VALUES (20004, 'XmlBulkResource',1,'2014-09-24 08:33:27','','2011-12-28 21:22:32','','2015-06-10 10:10:00','10 10 10 6 *','2015-06-10 10:32:11','Demo West Town OAI-PMH'        ,'OK','',NULL, 'Index Data',NULL,NULL,0,NULL,NULL,NULL,NULL,'','http://localhost:8080/test/alma_oai_pmh_west_town.xml',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,204,2002,1,'2','1000','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,0,1,60,2,60,'DEBUG','WARN','',NULL,0,0,NULL,1,2,1,NULL,'{\r\n\ \"folioAuthPath\": \"/bl-users/login\",\r\n \"folioTenant\": \"diku\",\r\n \"folioUsername\": \"diku_admin\",\r\n \"folioPassword\": \"admin\",\r\n \"instanceStoragePath\": \"/instance-storage-match/instances\",\r\n \"holdingsStoragePath\": \"/holdings-storage/holdings\",\r\n \"itemStoragePath\": \"/item-storage/items\"\r\n}',0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `HARVESTABLE` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `STEP` WRITE;
/*!40000 ALTER TABLE `STEP` DISABLE KEYS */;
INSERT INTO `STEP` VALUES (10001,'CustomTransformationStep','PZ to FOLIO Instance JSON',1,'PZ to Instance',NULL,'custom','JSON','XML','com.indexdata.masterkey.localindices.harvest.messaging.Pz2XmlToInstanceJsonTransformerRouter',NULL,NULL),(10002,'XmlTransformationStep','Maps to FOLIO identifierTypeIds, permanentLocationIds',NULL,'Library codes to FOLIO UUIDs','<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\r\n                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\">\r\n  <xsl:template match=\"@* | node()\">\r\n    <xsl:copy>\r\n      <xsl:apply-templates select=\"@* | node()\"/>\r\n    </xsl:copy>\r\n  </xsl:template>\r\n\r\n  <!-- Map legacy code for the library/institution to a FOLIO resource identifier\r\n       type UUID. Used for qualifying a local record identifier with the library\r\n       it originated from in context of a shared index setup where the Instance\r\n       represents bib records from multiple libraries. The UUID should exist in \r\n       the resource identifiers reference table.\r\n  -->\r\n  <xsl:template match=\"identifiers/arr/i/identifierType\">\r\n    <xsl:choose>\r\n      <xsl:when test=\".=\'EAST\'\">\r\n        <identifierTypeId>47a65482-f104-45e8-aead-1f12d70dcf32</identifierTypeId>\r\n      </xsl:when>\r\n      <xsl:when test=\".=\'WEST\'\">\r\n        <identifierTypeId>9db07825-8035-4d9a-8a41-d59a5f1c337b</identifierTypeId>\r\n      </xsl:when>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n\r\n  <!-- Map legacy location code to a FOLIO location UUID -->\r\n  <xsl:template match=\"holdingsRecords/arr/i/permanentLocation\">\r\n    <xsl:choose>\r\n      <xsl:when test=\".=\'EAST\'\">\r\n        <permanentLocationId>81582666-305d-4c8e-82cc-061fd00e9c42</permanentLocationId>\r\n      </xsl:when>\r\n      <xsl:when test=\".=\'WEST\'\">\r\n        <permanentLocationId>d05b8941-a7b3-4519-b450-06d72ca13a0c</permanentLocationId>\r\n      </xsl:when>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n</xsl:stylesheet>\r\n',NULL,'XML','XML',NULL,'<a>\r\n <b>C</b>\r\n</a>','<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>\r\n <b>C</b>\r\n</a>'),(10003,'CustomTransformationStep','FOLIO Instance XML to JSON',1,'Instance XML to JSON','','custom','JSON','XML','com.indexdata.masterkey.localindices.harvest.messaging.InstanceXmlToInstanceJsonTransformerRouter','',''),(10004,'XmlTransformationStep','MARC21 XML to FOLIO Instance XML',NULL,'MARC21 to Instance XML','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<xsl:stylesheet\r\n    version=\"1.0\"\r\n    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\r\n    xmlns:marc=\"http://www.loc.gov/MARC21/slim\">\r\n\r\n  <xsl:import href=\"map-relator-to-contributor-type.xsl\"/>\r\n\r\n  <xsl:output indent=\"yes\" method=\"xml\" version=\"1.0\" encoding=\"UTF-8\"/>\r\n\r\n<!-- Extract metadata from MARC21/USMARC\r\n      http://www.loc.gov/marc/bibliographic/ecbdhome.html\r\n-->\r\n\r\n  <xsl:template match=\"/\">\r\n    <collection>\r\n      <xsl:apply-templates />\r\n    </collection>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"//delete\">\r\n     <xsl:copy-of select=\".\"/>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"//marc:record\">\r\n\r\n    <record>\r\n      <source>MARC</source>\r\n\r\n      <!-- Instance type ID (resource type) -->\r\n      <instanceTypeId>\r\n        <!-- UUIDs for resource types -->\r\n        <xsl:choose>\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'a\'\">6312d172-f0cf-40f6-b27d-9fa8feaf332f</xsl:when> <!-- language material : text -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'c\'\">497b5090-3da2-486c-b57f-de5bb3c2e26d</xsl:when> <!-- notated music : notated music -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'d\'\">497b5090-3da2-486c-b57f-de5bb3c2e26d</xsl:when> <!-- manuscript notated music : notated music -> notated music -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'e\'\">526aa04d-9289-4511-8866-349299592c18</xsl:when> <!-- cartographic material : cartographic image -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'f\'\"></xsl:when>                                     <!-- manuscript cartographic material : ? -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'g\'\">535e3160-763a-42f9-b0c0-d8ed7df6e2a2</xsl:when> <!-- projected image : still image -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'i\'\">9bce18bd-45bf-4949-8fa8-63163e4b7d7f</xsl:when> <!-- nonmusical sound recording : sounds -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'j\'\">3be24c14-3551-4180-9292-26a786649c8b</xsl:when> <!-- musical sound recording : performed music -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'k\'\"></xsl:when>                                     <!-- two-dimensional nonprojectable graphic : ?-->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'m\'\">df5dddff-9c30-4507-8b82-119ff972d4d7</xsl:when> <!-- computer file : computer dataset -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'o\'\">a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:when> <!-- kit : other -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'p\'\">a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:when> <!-- mixed material : other -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'r\'\">c1e95c2b-4efc-48cf-9e71-edb622cf0c22</xsl:when> <!-- three-dimensional artifact or naturally occurring object : three-dimensional form -->\r\n          <xsl:when test=\"substring(marc:leader,7,1)=\'t\'\">6312d172-f0cf-40f6-b27d-9fa8feaf332f</xsl:when> <!-- manuscript language material : text -->\r\n          <xsl:otherwise>a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:otherwise>                             <!--  : other -->\r\n        </xsl:choose>\r\n      </instanceTypeId>\r\n\r\n      <!-- Identifiers -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'010\' or @tag=\'020\' or @tag=\'022\' or @tag=\'024\' or @tag=\'028\' or @tag=\'035\' or @tag=\'074\']\r\n                   or marc:controlfield[@tag=\'001\']\">\r\n        <identifiers>\r\n          <arr>\r\n          <xsl:for-each select=\"marc:controlfield[@tag=\'001\']\">\r\n            <i>\r\n              <value><xsl:value-of select=\".\"/></value>\r\n              <identifierType>\r\n                <xsl:value-of select=\"../marc:datafield[@tag=\'900\']/marc:subfield[@code=\'b\']\"/>\r\n              </identifierType>\r\n            </i>\r\n          </xsl:for-each>\r\n          <xsl:for-each select=\"marc:datafield[@tag=\'001\' or @tag=\'010\' or @tag=\'020\' or @tag=\'022\' or @tag=\'024\' or @tag=\'028\' or @tag=\'035\' or @tag=\'074\']\">\r\n            <i>\r\n              <xsl:choose>\r\n                <xsl:when test=\"current()[@tag=\'010\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>c858e4f2-2b6b-4385-842b-60732ee14abb</identifierTypeId> <!-- LCCN -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'020\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>8261054f-be78-422d-bd51-4ed9f33c3422</identifierTypeId> <!-- ISBN -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'022\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>913300b2-03ed-469a-8179-c1092c991227</identifierTypeId> <!-- ISSN -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'024\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>2e8b3b6c-0e7d-4e48-bca2-b0b23b376af5</identifierTypeId> <!-- Other standard identifier -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'028\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>b5d8cdc4-9441-487c-90cf-0c7ec97728eb</identifierTypeId> <!-- Publisher number -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'035\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>7e591197-f335-4afb-bc6d-a6d76ca3bace</identifierTypeId> <!-- System control number -->\r\n                </xsl:when>\r\n                <xsl:when test=\"current()[@tag=\'074\'] and marc:subfield[@code=\'a\']\">\r\n                  <value>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </value>\r\n                  <identifierTypeId>351ebc1c-3aae-4825-8765-c6d50dbf011f</identifierTypeId> <!-- GPO item number -->\r\n                </xsl:when>\r\n              </xsl:choose>\r\n            </i>\r\n          </xsl:for-each>\r\n          </arr>\r\n        </identifiers>\r\n      </xsl:if>\r\n\r\n      <!-- Classifications -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'050\' or @tag=\'060\' or @tag=\'080\' or @tag=\'082\' or @tag=\'086\' or @tag=\'090\']\">\r\n        <classifications>\r\n          <arr>\r\n            <xsl:for-each select=\"marc:datafield[@tag=\'050\' or @tag=\'060\' or @tag=\'080\' or @tag=\'082\' or @tag=\'086\' or @tag=\'090\']\">\r\n              <i>\r\n                <xsl:choose>\r\n                  <xsl:when test=\"current()[@tag=\'050\']\">\r\n                    <classificationNumber>\r\n                      <xsl:for-each select=\"marc:subfield[@code=\'a\' or @code=\'b\']\">\r\n                        <xsl:if test=\"position() > 1\">\r\n                        <xsl:text>; </xsl:text>\r\n                      </xsl:if>\r\n                      <xsl:value-of select=\".\"/>\r\n                      </xsl:for-each>\r\n                    </classificationNumber>\r\n                    <classificationTypeId>ce176ace-a53e-4b4d-aa89-725ed7b2edac</classificationTypeId> <!-- LC, Library of Congress -->\r\n                  </xsl:when>\r\n                  <xsl:when test=\"current()[@tag=\'082\']\">\r\n                    <classificationNumber>\r\n                      <xsl:for-each select=\"marc:subfield[@code=\'a\' or @code=\'b\']\">\r\n                        <xsl:if test=\"position() > 1\">\r\n                        <xsl:text>; </xsl:text>\r\n                      </xsl:if>\r\n                      <xsl:value-of select=\".\"/>\r\n                      </xsl:for-each>\r\n                    </classificationNumber>\r\n                    <classificationTypeId>42471af9-7d25-4f3a-bf78-60d29dcf463b</classificationTypeId> <!-- Dewey -->\r\n                  </xsl:when>\r\n                  <xsl:when test=\"current()[@tag=\'086\']\">\r\n                    <classificationNumber>\r\n                      <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                    </classificationNumber>\r\n                    <classificationTypeId>9075b5f8-7d97-49e1-a431-73fdd468d476</classificationTypeId> <!-- SUDOC -->\r\n                  </xsl:when>\r\n                </xsl:choose>\r\n              </i>\r\n            </xsl:for-each>\r\n          </arr>\r\n        </classifications>\r\n      </xsl:if>\r\n\r\n      <!-- title -->\r\n      <xsl:for-each select=\"marc:datafield[@tag=\'245\']\">\r\n        <title>\r\n          <xsl:call-template name=\"remove-characters-last\">\r\n            <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'a\']\" />\r\n            <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n          </xsl:call-template>\r\n          <xsl:if test=\"marc:subfield[@code=\'b\']\">\r\n           <xsl:text> : </xsl:text>\r\n            <xsl:call-template name=\"remove-characters-last\">\r\n              <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'b\']\" />\r\n              <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n            </xsl:call-template>\r\n          </xsl:if>\r\n          <xsl:if test=\"marc:subfield[@code=\'h\']\">\r\n            <xsl:text> </xsl:text>\r\n            <xsl:call-template name=\"remove-characters-last\">\r\n              <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'h\']\" />\r\n              <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n            </xsl:call-template>\r\n          </xsl:if>\r\n        </title>\r\n      </xsl:for-each>\r\n\r\n      <matchKey>\r\n        <xsl:for-each select=\"marc:datafield[@tag=\'245\']\">\r\n          <title>\r\n            <xsl:call-template name=\"remove-characters-last\">\r\n              <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'a\']\" />\r\n              <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n            </xsl:call-template>\r\n          </title>\r\n          <remainder-of-title>\r\n           <xsl:text> : </xsl:text>\r\n            <xsl:call-template name=\"remove-characters-last\">\r\n              <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'b\']\" />\r\n              <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n            </xsl:call-template>\r\n          </remainder-of-title>\r\n          <medium>\r\n            <xsl:call-template name=\"remove-characters-last\">\r\n              <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'h\']\" />\r\n              <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n            </xsl:call-template>\r\n          </medium>\r\n          <!-- Only fields that are actually included in\r\n               the instance somewhere - for example in \'title\' -\r\n               should be included as \'matchKey\' elements lest\r\n               the instance \"magically\" splits on \"invisible\"\r\n               properties.\r\n          <name-of-part-section-of-work>\r\n            <xsl:value-of select=\"marc:subfield[@code=\'p\']\" />\r\n          </name-of-part-section-of-work>\r\n          <number-of-part-section-of-work>\r\n            <xsl:value-of select=\"marc:subfield[@code=\'n\']\" />\r\n          </number-of-part-section-of-work>\r\n          <inclusive-dates>\r\n            <xsl:value-of select=\"marc:subfield[@code=\'f\']\" />\r\n          </inclusive-dates> -->\r\n        </xsl:for-each>\r\n      </matchKey>\r\n\r\n      <!-- Contributors -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'100\' or @tag=\'110\' or @tag=\'111\' or @tag=\'700\' or @tag=\'710\' or @tag=\'711\']\">\r\n        <contributors>\r\n          <arr>\r\n            <xsl:for-each select=\"marc:datafield[@tag=\'100\' or @tag=\'110\' or @tag=\'111\' or @tag=\'700\' or @tag=\'710\' or @tag=\'711\']\">\r\n              <i>\r\n                <name>\r\n                <xsl:for-each select=\"marc:subfield[@code=\'a\' or @code=\'b\' or @code=\'c\' or @code=\'d\' or @code=\'f\' or @code=\'g\' or @code=\'j\' or @code=\'k\' or @code=\'l\' or @code=\'n\' or @code=\'p\' or @code=\'q\' or @code=\'t\' or @code=\'u\']\">\r\n                  <xsl:if test=\"position() > 1\">\r\n                    <xsl:text>; </xsl:text>\r\n                  </xsl:if>\r\n                  <xsl:call-template name=\"remove-characters-last\">\r\n                    <xsl:with-param  name=\"input\" select=\".\" />\r\n                    <xsl:with-param  name=\"characters\">,-.</xsl:with-param>\r\n                  </xsl:call-template>\r\n                </xsl:for-each>\r\n                </name>\r\n                <xsl:choose>\r\n                  <xsl:when test=\"@tag=\'100\' or @tag=\'700\'\">\r\n                    <contributorNameTypeId>2b94c631-fca9-4892-a730-03ee529ffe2a</contributorNameTypeId> <!-- personal name -->\r\n                    <xsl:if test=\"@tag=\'100\'\">\r\n                      <primary>true</primary>\r\n                    </xsl:if>\r\n                  </xsl:when>\r\n                  <xsl:when test=\"@tag=\'110\' or @tag=\'710\'\">\r\n                    <contributorNameTypeId>2e48e713-17f3-4c13-a9f8-23845bb210aa</contributorNameTypeId> <!-- corporate name -->\r\n                  </xsl:when>\r\n                  <xsl:when test=\"@tag=\'111\' or @tage=\'711\'\">\r\n                    <contributorNameTypeId>e8b311a6-3b21-43f2-a269-dd9310cb2d0a</contributorNameTypeId> <!-- meeting name -->\r\n                  </xsl:when>\r\n                </xsl:choose>\r\n                <xsl:if test=\"marc:subfield[@code=\'e\' or @code=\'4\']\">\r\n                  <contributorTypeId>\r\n                    <xsl:call-template name=\"map-relator\"/>\r\n                  </contributorTypeId>\r\n                </xsl:if>\r\n              </i>\r\n            </xsl:for-each>\r\n          </arr>\r\n        </contributors>\r\n      </xsl:if>\r\n\r\n      <!-- Editions -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'250\']\">\r\n        <editions>\r\n          <arr>\r\n          <xsl:for-each select=\"marc:datafield[@tag=\'250\']\">\r\n            <i>\r\n              <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n              <xsl:if test=\"marc:subfield[@code=\'b\']\">; <xsl:value-of select=\"marc:subfield[@code=\'b\']\"/></xsl:if>\r\n            </i>\r\n          </xsl:for-each>\r\n          </arr>\r\n        </editions>\r\n      </xsl:if>\r\n\r\n      <!-- Publication -->\r\n      <xsl:choose>\r\n        <xsl:when test=\"marc:datafield[@tag=\'260\' or @tag=\'264\']\">\r\n          <publication>\r\n            <arr>\r\n              <xsl:for-each select=\"marc:datafield[@tag=\'260\' or @tag=\'264\']\">\r\n                <i>\r\n                  <publisher>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'b\']\"/>\r\n                  </publisher>\r\n                  <place>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'a\']\"/>\r\n                  </place>\r\n                  <dateOfPublication>\r\n                    <xsl:value-of select=\"marc:subfield[@code=\'c\']\"/>\r\n                  </dateOfPublication>\r\n                </i>\r\n              </xsl:for-each>\r\n            </arr>\r\n          </publication>\r\n        </xsl:when>\r\n        <xsl:otherwise>\r\n          <publication>\r\n            <arr>\r\n              <i>\r\n                <dateOfPublication>\r\n                  <xsl:value-of select=\"substring(marc:controlfield[@tag=\'008\'],8,4)\"/>\r\n                </dateOfPublication>\r\n              </i>\r\n            </arr>\r\n          </publication>\r\n        </xsl:otherwise>\r\n      </xsl:choose>\r\n\r\n      <!-- physicalDescriptions -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'300\']\">\r\n        <physicalDescriptions>\r\n          <arr>\r\n            <xsl:for-each select=\"marc:datafield[@tag=\'300\']\">\r\n              <i>\r\n                <xsl:call-template name=\"remove-characters-last\">\r\n                  <xsl:with-param  name=\"input\" select=\"marc:subfield[@code=\'a\']\" />\r\n                  <xsl:with-param  name=\"characters\">,-./ :;</xsl:with-param>\r\n                </xsl:call-template>\r\n              </i>\r\n            </xsl:for-each>\r\n          </arr>\r\n        </physicalDescriptions>\r\n      </xsl:if>\r\n\r\n      <!-- Subjects -->\r\n      <xsl:if test=\"marc:datafield[@tag=\'600\' or @tag=\'610\' or @tag=\'611\' or @tag=\'630\' or @tag=\'648\' or @tag=\'650\' or @tag=\'651\' or @tag=\'653\' or @tag=\'654\' or @tag=\'655\' or @tag=\'656\' or @tag=\'657\' or @tag=\'658\' or @tag=\'662\' or @tag=\'69X\']\">\r\n        <subjects>\r\n          <arr>\r\n          <xsl:for-each select=\"marc:datafield[@tag=\'600\' or @tag=\'610\' or @tag=\'611\' or @tag=\'630\' or @tag=\'648\' or @tag=\'650\' or @tag=\'651\' or @tag=\'653\' or @tag=\'654\' or @tag=\'655\' or @tag=\'656\' or @tag=\'657\' or @tag=\'658\' or @tag=\'662\' or @tag=\'69X\']\">\r\n            <i>\r\n            <xsl:for-each select=\"marc:subfield[@code=\'a\' or @code=\'b\' or @code=\'c\' or @code=\'d\' or @code=\'f\' or @code=\'g\' or @code=\'j\' or @code=\'k\' or @code=\'l\' or @code=\'n\' or @code=\'p\' or @code=\'q\' or @code=\'t\' or @code=\'u\' or @code=\'v\' or @code=\'z\']\">\r\n              <xsl:if test=\"position() > 1\">\r\n                <xsl:text>--</xsl:text>\r\n              </xsl:if>\r\n              <xsl:call-template name=\"remove-characters-last\">\r\n                  <xsl:with-param  name=\"input\" select=\".\" />\r\n                  <xsl:with-param  name=\"characters\">,-.</xsl:with-param>\r\n                </xsl:call-template>\r\n            </xsl:for-each>\r\n            </i>\r\n          </xsl:for-each>\r\n          </arr>\r\n        </subjects>\r\n      </xsl:if>\r\n\r\n      <!-- holdings and items -->\r\n      <xsl:choose>\r\n        <xsl:when test=\"marc:datafield[@tag=\'900\']\">\r\n          <holdingsRecords>\r\n             <arr>\r\n               <i>\r\n                 <xsl:for-each select=\"marc:datafield[@tag=\'900\']\">\r\n                   <permanentLocation><xsl:value-of select=\"marc:subfield[@code=\'b\']\"/></permanentLocation>\r\n                   <callNumber>\r\n                     <xsl:value-of select=\"marc:subfield[@code=\'h\']\"/>\r\n                   </callNumber>\r\n                   <items>\r\n                     <arr>\r\n                       <i>\r\n                         <barcode>\r\n                           <xsl:value-of select=\"marc:subfield[@code=\'8\']\"/>\r\n                         </barcode>\r\n                         <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId> <!-- Can circulate -->\r\n                         <materialTypeId>1a54b431-2e4f-452d-9cae-9cee66c9a892</materialTypeId>           <!-- Book -->\r\n                         <status>\r\n                           <name>Available</name>\r\n                         </status>\r\n                       </i>\r\n                     </arr>\r\n                   </items>\r\n                 </xsl:for-each>\r\n               </i>\r\n             </arr>\r\n          </holdingsRecords>\r\n        </xsl:when>\r\n        <xsl:otherwise>\r\n          <holdingsRecords>\r\n            <arr>\r\n              <i>\r\n                <items>\r\n                  <arr>\r\n                    <i>\r\n                      <barcode>\r\n                        <xsl:value-of select=\"marc:subfield[@code=\'8\']\"/>\r\n                      </barcode>\r\n                      <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId> <!-- Can circulate -->\r\n                      <materialTypeId>1a54b431-2e4f-452d-9cae-9cee66c9a892</materialTypeId>           <!-- Book -->\r\n                      <status>\r\n                        <name>Available</name>\r\n                      </status>\r\n                    </i>\r\n                  </arr>\r\n                </items>\r\n              </i>\r\n            </arr>\r\n          </holdingsRecords>\r\n        </xsl:otherwise>\r\n      </xsl:choose>\r\n    </record>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"text()\"/>\r\n\r\n  <xsl:template name=\"remove-characters-last\">\r\n    <xsl:param name=\"input\" />\r\n    <xsl:param name=\"characters\"/>\r\n    <xsl:variable name=\"lastcharacter\" select=\"substring($input,string-length($input))\" />\r\n    <xsl:choose>\r\n      <xsl:when test=\"$characters and $lastcharacter and contains($characters, $lastcharacter)\">\r\n        <xsl:call-template name=\"remove-characters-last\">\r\n          <xsl:with-param  name=\"input\" select=\"substring($input,1, string-length($input)-1)\" />\r\n          <xsl:with-param  name=\"characters\" select=\"$characters\" />\r\n        </xsl:call-template>\r\n      </xsl:when>\r\n      <xsl:otherwise>\r\n        <xsl:value-of select=\"$input\"/>\r\n      </xsl:otherwise>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n\r\n</xsl:stylesheet>\r\n',NULL,'XML','XML',NULL,'','');
/*!40000 ALTER TABLE `STEP` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `TRANSFORMATION` WRITE;
/*!40000 ALTER TABLE `TRANSFORMATION` DISABLE KEYS */;
INSERT INTO `TRANSFORMATION` VALUES
(2001, 'BasicTransformation','OAI-PMH(DC) to FOLIO Instance JSON via PZ',1,'OAI-PMH(DC) to Instance JSON',NULL);
INSERT INTO `TRANSFORMATION` VALUES
(2002, 'BasicTransformation','OAI-PMH MARC21 to FOLIO Instance JSON',1,'OAI-PMH MARC21 to Instance JSON',NULL);
/*!40000 ALTER TABLE `TRANSFORMATION` ENABLE KEYS */;
UNLOCK TABLES;


LOCK TABLES `TRANSFORMATION_STEP` WRITE;
/*!40000 ALTER TABLE `TRANSFORMATION_STEP` DISABLE KEYS */;

-- Pipeline for creating instance JSON from OAI-PMH DC
INSERT INTO `TRANSFORMATION_STEP` VALUES
  (20001,'TransformationStepAssociation',1,2001,10),
  (20003,'TransformationStepAssociation',3,2001,10001);

-- Pipeline for creating instance JSON from OAI-PMH MARC21
INSERT INTO `TRANSFORMATION_STEP` VALUES
   (20004,'TransformationStepAssociation',1,2002,10004),
   (20005,'TransformationStepAssociation',2,2002,10002),
   (20006,'TransformationStepAssociation',3,2002,10003);

/*!40000 ALTER TABLE `TRANSFORMATION_STEP` ENABLE KEYS */;
UNLOCK TABLES;




/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

