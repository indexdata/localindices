/*
 * Add new CONSTANTFIELDS column, consisting of a comma-separated list 
 * of NAME=VALUE pairs. For a harvestable that has this field set, 
 * each harvested record has each NAME field set to the corresponding 
 * VALUE. Needed for MKH-513.
 */
alter table `HARVESTABLE` add column `CONSTANTFIELDS` varchar(1024) NULL;
