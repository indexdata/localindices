-- there are problems with storing utf8 strings in the table
-- in order to convert the table we need to modify fields
-- Note 1: check columns charset with
-- SHOW FULL COLUMNS FROM HARVESTABLE;
-- Note 2: to set the default for new tables use:
-- ALTER DATABASE localindices CHARACTER SET utf8 COLLATE utf8_general_ci;
-- Note 3: check the server/db defaults with
-- SHOW VARIABLES LIKE 'char%';
-- Note 4: it is recomended to use utf8_unicode_ci instead utf8_general_ci
-- which is more precise

ALTER TABLE HARVESTABLE MODIFY DESCRIPTION TEXT;
ALTER TABLE HARVESTABLE MODIFY TECHNICALNOTES TEXT;
ALTER TABLE HARVESTABLE MODIFY CONTACTNOTES TEXT;
ALTER TABLE HARVESTABLE MODIFY MESSAGE TEXT;
ALTER TABLE HARVESTABLE MODIFY STARTURLS TEXT;
ALTER TABLE HARVESTABLE MODIFY URL TEXT;
ALTER TABLE HARVESTABLE MODIFY INITDATA TEXT;
ALTER TABLE HARVESTABLE MODIFY CONNECTORURL TEXT;
ALTER TABLE HARVESTABLE CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
