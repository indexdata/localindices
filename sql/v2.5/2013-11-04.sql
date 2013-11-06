CREATE TABLE `SETTING` (
  `ID` bigint(20) NOT NULL,
  `NAME` varchar(4096) NOT NULL,
  `LABEL` varchar(4096) NOT NULL,
  `VALUE` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `SETTING` VALUES (1, "cf.engine.url.1", "CF Engine (default)", "http://connect.indexdata.com:9010/connector");

INSERT INTO `SETTING` VALUES (2, "cf.repo.url.1", "CF Repo (default)", "https://idtest:idtest36@cfrepo.indexdata.com/repo.pl/idtest/");

alter table `HARVESTABLE` add column `CONNECTORENGINEURLSETTING_ID` bigint(20) DEFAULT NULL;

UPDATE `HARVESTABLE` SET `CONNECTORENGINEURLSETTING_ID`=1;

alter table `HARVESTABLE` add column `CONNECTORREPOURLSETTING_ID` bigint(20) DEFAULT NULL;

UPDATE `HARVESTABLE` SET `CONNECTORREPOURLSETTING_ID`=2;
