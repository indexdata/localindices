-- corresponds to v1 changes
USE localindices;
 ALTER TABLE HARVESTABLE ADD COLUMN
  INITIALLYHARVESTED DATETIME AFTER LASTUPDATED;
 ALTER TABLE HARVESTABLE ADD COLUMN
  LASTHARVESTFINISHED DATETIME AFTER LASTHARVESTSTARTED;
 ALTER TABLE HARVESTABLE CHANGE COLUMN
  TITLE SERVICEPROVIDER VARCHAR(255);
 ALTER TABLE HARVESTABLE ADD COLUMN
  TECHNICALNOTES VARCHAR(4096) AFTER DESCRIPTION;
 ALTER TABLE HARVESTABLE ADD COLUMN
  CONTACTNOTES VARCHAR(4096) AFTER TECHNICALNOTES;
 ALTER TABLE HARVESTABLE CHANGE COLUMN
  ERROR MESSAGE VARCHAR(4096);
 ALTER TABLE HARVESTABLE CHANGE COLUMN
  RECORDSHARVESTED AMOUNTHARVESTED INT(11);


