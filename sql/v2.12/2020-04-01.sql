/**
 * Author:  ne
 * Created: Mar 20, 2020
 */

ALTER TABLE `HARVESTABLE` ADD COLUMN `ACL` TEXT;

ALTER TABLE `TRANSFORMATION` ADD COLUMN `ACL` TEXT;

ALTER TABLE `STEP` ADD COLUMN `ACL` TEXT;

ALTER TABLE `STORAGE` ADD COLUMN `ACL` TEXT;

ALTER TABLE `SETTING` ADD COLUMN `ACL` TEXT;

