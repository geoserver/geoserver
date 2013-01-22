DROP VIEW service;
DROP VIEW settings;
DROP VIEW global;
DROP VIEW layergroup_style;
DROP VIEW layergroup_layer;
DROP VIEW layergroup;
DROP VIEW layer_style;
DROP VIEW layer;
DROP VIEW style;
DROP VIEW wmslayer;
DROP VIEW wmsstore;
DROP VIEW coverage;
DROP VIEW coveragestore;
DROP VIEW featuretype;
DROP VIEW datastore;
DROP VIEW workspace;

ALTER TABLE object_property DROP CONSTRAINT fk_object_property;
ALTER TABLE property_type DROP CONSTRAINT fk_type_property_type;
ALTER TABLE object DROP CONSTRAINT fk_object_type;
ALTER TABLE property_type DROP CONSTRAINT fk_property_type_target_property;
ALTER TABLE object_property DROP CONSTRAINT fk_object_property_property_type;

DROP TABLE object CASCADE;
DROP TABLE object_property CASCADE;
DROP TABLE type CASCADE;
DROP TABLE property_type CASCADE;
DROP TABLE default_object CASCADE;
