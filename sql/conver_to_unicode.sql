-- there are problems with storing utf8 strings in the table
-- in order to convert the table we need to modify fields
-- check charset with: show full columns from HARVESTABLE;
-- Note: to set the default for new tables use:
-- ALTER DATABASE localindices CHARACTER SET utf8 COLLATE utf8_general_ci;
-- Note2: it is recomended to use utf8_unicode_ci instead utf8_general_ci
-- whic is more precise

ALTER TABLE HARVESTABLE MODIFY DESCRIPTION TEXT;
ALTER TABLE HARVESTABLE MODIFY TECHNICALNOTES TEXT;
ALTER TABLE HARVESTABLE MODIFY CONTACTNOTES TEXT;
ALTER TABLE HARVESTABLE MODIFY MESSAGE TEXT;
ALTER TABLE HARVESTABLE MODIFY STARTURLS TEXT;
ALTER TABLE HARVESTABLE MODIFY URL TEXT;
ALTER TABLE HARVESTABLE MODIFY INITDATA TEXT;
ALTER TABLE HARVESTABLE MODIFY CONNECTORURL TEXT;
ALTER TABLE HARVESTABLE CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
