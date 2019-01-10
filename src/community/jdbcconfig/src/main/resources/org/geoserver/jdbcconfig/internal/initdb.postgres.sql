-- tables
CREATE TABLE object ( 
  oid serial NOT NULL, 
  type_id int4 NOT NULL, 
  id text NOT NULL, 
  blob text NOT NULL, 
  PRIMARY KEY (oid)
);

CREATE TABLE object_property (
  oid int4 NOT NULL, 
  property_type int4 NOT NULL, 
  id text NOT NULL, 
  related_oid int4, 
  related_property_type int4, 
  colindex int4 NOT NULL, 
  value text, 
  PRIMARY KEY (oid, property_type, colindex)
);

CREATE TABLE type (
  oid serial NOT NULL,
  typename text NOT NULL, 
  PRIMARY KEY (OID)
);

CREATE TABLE property_type (
  oid  serial NOT NULL, 
  target_property int4, 
  type_id int4 NOT NULL, 
  name text NOT NULL, 
  collection bool NOT NULL, 
  text bool NOT NULL, 
  PRIMARY KEY (oid)
);

CREATE TABLE default_object (
  def_key text NOT NULL, 
  id text NOT NULL

);

-- foreign keys
ALTER TABLE object_property ADD CONSTRAINT fk_object_property 
  FOREIGN KEY (oid) REFERENCES object (oid) ON DELETE CASCADE;

ALTER TABLE property_type ADD CONSTRAINT 
  fk_type_property_type FOREIGN KEY (type_id) REFERENCES type (oid);

ALTER TABLE object ADD CONSTRAINT 
  fk_object_type FOREIGN KEY (type_id) REFERENCES type (oid);

ALTER TABLE property_type ADD CONSTRAINT 
  fk_property_type_target_property FOREIGN KEY (target_property) REFERENCES property_type (oid);

ALTER TABLE object_property ADD CONSTRAINT 
  fk_object_property_property_type FOREIGN KEY (property_type) REFERENCES property_type (oid);

-- indexes
CREATE INDEX object_type_id_idx ON object (type_id);
CREATE UNIQUE INDEX object_id_idx ON object (id);

CREATE INDEX object_property_value_upper_idx ON object_property (UPPER(value));
CREATE INDEX object_property_property_type_idx ON object_property (property_type);
CREATE INDEX object_property_id_idx ON object_property (id);
CREATE INDEX object_property_related_oid_idx ON object_property (related_oid);
CREATE INDEX object_property_related_property_type_idx ON object_property (related_property_type);
CREATE INDEX object_property_colindex_idx ON object_property (colindex);
CREATE INDEX object_property_value_idx ON object_property (value);

CREATE UNIQUE INDEX type_typename_idx ON type (typename);

CREATE INDEX property_type_target_property_idx ON property_type (target_property);
CREATE INDEX property_type_type_id_idx ON property_type (type_id);
CREATE INDEX property_type_name_idx ON property_type (name);
CREATE INDEX property_type_collection_idx ON property_type (collection);

CREATE INDEX default_object_def_key_idx ON default_object (def_key);
CREATE INDEX default_object_id_idx ON default_object (id);

-- views
-- workspace view 
CREATE OR REPLACE VIEW workspace AS
SELECT a.oid, 
       a.id, 
       (SELECT c.value 
          FROM object_property c, property_type d 
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name, 
       (SELECT e.value
          FROM object_property e, property_type f
         WHERE e.property_type = f.oid
           AND e.oid = (SELECT g.oid 
                          FROM object_property g, property_type h 
                         WHERE g.property_type = h.oid
                           AND g.value = (SELECT i.value
                                            FROM object_property i, property_type j
                                           WHERE i.oid = a.oid
                                             AND i.property_type = j.oid
                                             AND j.name = 'name')
                           AND h.name = 'prefix')
           AND f.name = 'URI') as uri
  FROM object a, type b
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.WorkspaceInfo'; 
     
-- datastore view
CREATE OR REPLACE VIEW datastore AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'description') as description,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'type') as type,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.DataStoreInfo';

-- feature type view
CREATE OR REPLACE VIEW featuretype AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'nativeName') as native_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'prefixedName') as prefixed_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'SRS') as srs,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'projectionPolicy') as projection_policy,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'store.id') store,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'namespace.id') namespace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.FeatureTypeInfo';

-- coveragestore view
CREATE OR REPLACE VIEW coveragestore AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'description') as description,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'type') as type,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.CoverageStoreInfo';

-- coverage view
CREATE OR REPLACE VIEW coverage AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'nativeName') as native_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'prefixedName') as prefixed_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'SRS') as srs,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'projectionPolicy') as projection_policy,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'store.id') store,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'namespace.id') namespace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.CoverageInfo';

-- wmsstore view
CREATE OR REPLACE VIEW wmsstore AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'description') as description,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'capabilitiesURL') as capabilities_url,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'type') as type,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.WMSStoreInfo';

-- wms layer view
CREATE OR REPLACE VIEW wmslayer AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'nativeName') as native_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'prefixedName') as prefixed_name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'SRS') as srs,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'projectionPolicy') as projection_policy,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'store.id') store,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'namespace.id') namespace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.WMSLayerInfo';

-- style view
CREATE OR REPLACE VIEW style AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'filename') as filename,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.StyleInfo';

-- layer view
CREATE OR REPLACE VIEW layer AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'type') as type,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'defaultStyle.id') default_style,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'resource.id') resource
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.LayerInfo';

-- layergroup styles
CREATE OR REPLACE VIEW layer_style AS
SELECT a.oid, b.related_oid as style
  FROM object a, object_property b, property_type c, type d 
 WHERE a.oid = b.oid 
   AND a.type_id = d.oid
   AND b.property_type = c.oid
   AND c.name = 'styles.id'
   AND d.typename = 'org.geoserver.catalog.LayerInfo';

-- layer group view
CREATE OR REPLACE VIEW layergroup AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'mode') as mode,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.catalog.LayerGroupInfo';

-- layergroup layers
CREATE OR REPLACE VIEW layergroup_layer AS
SELECT a.oid, b.related_oid as layer
  FROM object a, object_property b, property_type c, type d 
 WHERE a.oid = b.oid 
   AND a.type_id = d.oid
   AND b.property_type = c.oid
   AND c.name = 'layers.id'
   AND d.typename = 'org.geoserver.catalog.LayerGroupInfo';
  
-- layergroup styles
CREATE OR REPLACE VIEW layergroup_style AS
SELECT a.oid, b.related_oid as style
  FROM object a, object_property b, property_type c, type d 
 WHERE a.oid = b.oid 
   AND a.type_id = d.oid
   AND b.property_type = c.oid
   AND c.name = 'styles.id'
   AND d.typename = 'org.geoserver.catalog.LayerGroupInfo';

-- global view
CREATE OR REPLACE VIEW global AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'featureTypeCacheSize') as feature_type_cache_size,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'globalServices') as global_services,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'xmlPostRequestLogBufferSize') as xml_post_request_log_buffer_size,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'updateSequence') as update_sequence,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'settings.id') as settings
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.config.GeoServerInfo';

-- settings view
CREATE OR REPLACE VIEW settings AS
SELECT a.oid,
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'charset') as charset,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'verbose') as verbose,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'verboseExceptions') as verbose_exceptions,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'numDecimals') as num_decimals,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'onlineResource') as online_resource,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'proxyBaseUrl') as proxy_base_url,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'schemaBaseUrl') as schema_base_url,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') as workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.config.SettingsInfo';

-- service view
CREATE OR REPLACE VIEW service AS
SELECT a.oid, 
       a.id,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'name') as name,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'title') as title,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'abstract') as abstract,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'maintainer') as maintainer,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'verbose') as verbose,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'citeCompliant') as cite_compliant,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'outputStrategy') as output_strategy,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'onlineResource') as online_resource,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'schemaBaseURL') as schema_base_url,
       (SELECT c.value
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'enabled') as enabled,
       (SELECT c.related_oid
          FROM object_property c, property_type d
         WHERE c.oid = a.oid
           AND c.property_type = d.oid
           AND d.name = 'workspace.id') as workspace
  FROM object a, type b  
 WHERE a.type_id = b.oid
   AND b.typename = 'org.geoserver.config.ServiceInfo';
