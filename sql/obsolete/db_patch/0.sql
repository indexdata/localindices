-- corresponds to v0 changes
USE localindices;
 ALTER TABLE HARVESTABLE CHANGE COLUMN
  DESCRIPTION DESCRIPTION VARCHAR(4096);
 ALTER TABLE HARVESTABLE CHANGE COLUMN
  URL URL VARCHAR(16384);