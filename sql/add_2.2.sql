alter table HARVESTABLE add column USERNAME varchar(255);
alter table HARVESTABLE add column PASSWORD varchar(255);
alter table HARVESTABLE add column PROXY    varchar(255);
alter table HARVESTABLE add column SCRIPT   varchar(102400);
alter table HARVESTABLE add column ISPERSISTENCE boolean;
alter table HARVESTABLE add column INITDATA varchar(4096);
alter table HARVESTABLE add column SLEEP    integer;
alter table TRANSFORMATION add column `PARALLEL` tinyint(1) DEFAULT NULL,

