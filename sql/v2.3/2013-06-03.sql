alter table HARVESTABLE add column CLEARRTONERROR tinyint(1) NOT NULL DEFAULT 0;
alter table HARVESTABLE add column KEEPPARTIAL tinyint(1) NOT NULL DEFAULT 1;
