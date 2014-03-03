alter table `STORAGE` add column TIMEOUT smallint(4) NOT NULL DEFAULT 60;
alter table `STORAGE` add column RETRYCOUNT tinyint(3) NOT NULL DEFAULT 2;
alter table `STORAGE` add column RETRYWAIT smallint(4) NOT NULL DEFAULT 60;