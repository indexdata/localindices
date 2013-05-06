-- Support for HarvestConnector
alter table HARVESTABLE add column USERNAME varchar(255);
alter table HARVESTABLE add column PASSWORD varchar(255);
alter table HARVESTABLE add column PROXY    varchar(255);
alter table HARVESTABLE add column ISPERSISTENCE boolean;
alter table HARVESTABLE add column INITDATA varchar(4096);
alter table HARVESTABLE add column SLEEP    integer;
alter table HARVESTABLE add column CONNECTORURL varchar(4096);
alter table HARVESTABLE add column ALLOWERRORS tinyint;
-- Support for parallel
alter table TRANSFORMATION add column `PARALLEL` tinyint(1) DEFAULT NULL;
-- Support for custom implementation
alter table STEP       add column `CUSTOMCLASS` varchar(255) DEFAULT NULL;
-- Fix change in class name
update STEP set dtype = 'XmlTransformationStep' where dtype = 'BasicTransformationStep';

