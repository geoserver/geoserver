DROP VIEW IF EXISTS service;
DROP VIEW IF EXISTS settings;
DROP VIEW IF EXISTS global;
DROP VIEW IF EXISTS layergroup_style;
DROP VIEW IF EXISTS layergroup_layer;
DROP VIEW IF EXISTS layergroup;
DROP VIEW IF EXISTS layer_style;
DROP VIEW IF EXISTS layer;
DROP VIEW IF EXISTS style;
DROP VIEW IF EXISTS wmslayer;
DROP VIEW IF EXISTS wmsstore;
DROP VIEW IF EXISTS coverage;
DROP VIEW IF EXISTS coveragestore;
DROP VIEW IF EXISTS featuretype;
DROP VIEW IF EXISTS datastore;
DROP VIEW IF EXISTS workspace;

? ALTER TABLE object_property DROP CONSTRAINT fk_object_property;
? ALTER TABLE property_type DROP CONSTRAINT fk_type_property_type;
? ALTER TABLE object DROP CONSTRAINT fk_object_type;
? ALTER TABLE property_type DROP CONSTRAINT fk_property_type_target_property;
? ALTER TABLE object_property DROP CONSTRAINT fk_object_property_property_type;

DROP TABLE IF EXISTS object CASCADE;
DROP TABLE IF EXISTS object_property CASCADE;
DROP TABLE IF EXISTS type CASCADE;
DROP TABLE IF EXISTS property_type CASCADE;
DROP TABLE IF EXISTS default_object CASCADE;
