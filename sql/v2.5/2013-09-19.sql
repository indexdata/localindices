alter table STORAGE add column CUSTOMCLASS varchar(1024);
alter table STORAGE add column LOGLEVEL varchar(30);
alter table STORAGE add column MAILLEVEL varchar(30);
alter table STORAGE add column MAILADDRESS varchar(1000);
alter table HARVESTABLE add column LOGLEVEL varchar(30);
alter table HARVESTABLE add column MAILLEVEL varchar(30);
alter table HARVESTABLE add column MAILADDRESS varchar(1000);

