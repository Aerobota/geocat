UPDATE Settings SET value='3.0.0' WHERE name='system/platform/version';
UPDATE Settings SET value='SNAPSHOT' WHERE name='system/platform/subVersion';

-- GEOCAT
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('region/getmap/background', 'geocat', 0, 9590, 'n');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('region/getmap/width', '500', 0, 9590, 'n');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('region/getmap/summaryWidth', '500', 0, 9590, 'n');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('region/getmap/mapproj', 'EPSG:21781', 0, 9590, 'n');
-- END GEOCAT

ALTER TABLE Validation DROP COLUMN Failed;
ALTER TABLE Validation DROP COLUMN Tested;
