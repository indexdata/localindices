alter table HARVESTABLE add column TIMEOUT smallint(4) NOT NULL DEFAULT 60;
alter table HARVESTABLE add column RETRYCOUNT tinyint(3) NOT NULL DEFAULT 2;
alter table HARVESTABLE add column RETRYWAIT smallint(4) NOT NULL DEFAULT 60;
