CREATE TABLE `SETTING` (
  `ID` bigint(20) NOT NULL,
  `NAME` varchar(4096) NOT NULL,
  `LABEL` varchar(4096) NOT NULL,
  `VALUE` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table `HARVESTABLE` add column `CONNECTORENGINEURLSETTING_ID` bigint(20) DEFAULT NULL;
alter table `HARVESTABLE` add column `CONNECTORREPOURLSETTING_ID` bigint(20) DEFAULT NULL;
