ALTER TABLE TRANSFORMATION_STEP DROP FOREIGN KEY FK_TRANSFORMATION_STEP_TRANSFORMATION_ID;
ALTER TABLE TRANSFORMATION_STEP DROP KEY UNQ_TRANSFORMATION_STEP_0;
ALTER TABLE TRANSFORMATION_STEP ADD KEY `TRANSFORMATION_STEP_TRANSFORMATION_ID` (`TRANSFORMATION_ID`);
ALTER TABLE TRANSFORMATION_STEP ADD CONSTRAINT `FK_TRANSFORMATION_STEP_TRANSFORMATION_ID` FOREIGN KEY (`TRANSFORMATION_ID`) REFERENCES `TRANSFORMATION` (`ID`);

ALTER TABLE TRANSFORMATION_STEP CHANGE TRANSFORMATION_ID TRANSFORMATION_ID bigint(20) NOT NULL;
ALTER TABLE TRANSFORMATION_STEP CHANGE STEP_ID STEP_ID bigint(20) NOT NULL;