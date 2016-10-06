CREATE SEQUENCE seq_OBJECT;
CREATE SEQUENCE seq_TYPE;
CREATE SEQUENCE seq_PROPERTY_TYPE;
CREATE TABLE OBJECT (OID number(10) NOT NULL, TYPE_ID number(10) NOT NULL, ID varchar2(255) NOT NULL, BLOB clob NOT NULL, PRIMARY KEY (OID));
CREATE TABLE OBJECT_PROPERTY (OID number(10) NOT NULL, PROPERTY_TYPE number(10) NOT NULL, ID varchar2(255) NOT NULL, RELATED_OID number(10), RELATED_PROPERTY_TYPE number(10), COLINDEX number(10) NOT NULL, VALUE varchar2(1023), PRIMARY KEY (OID, PROPERTY_TYPE, COLINDEX));
CREATE TABLE TYPE (OID number(10) NOT NULL, TYPENAME varchar2(255) NOT NULL, PRIMARY KEY (OID));
CREATE TABLE PROPERTY_TYPE (OID number(10) NOT NULL, TARGET_PROPERTY number(10), TYPE_ID number(10) NOT NULL, NAME varchar2(255) NOT NULL, COLLECTION number(1) NOT NULL, TEXT number(1) NOT NULL, PRIMARY KEY (OID));
CREATE TABLE DEFAULT_OBJECT (DEF_KEY varchar2(255) NOT NULL, ID varchar2(255) NOT NULL);
ALTER TABLE OBJECT_PROPERTY ADD CONSTRAINT FK_OBJECT_PROPERTY FOREIGN KEY (OID) REFERENCES OBJECT (OID) ON DELETE Cascade;
ALTER TABLE PROPERTY_TYPE ADD CONSTRAINT FK_TYPE_PROPERTY_TYPE FOREIGN KEY (TYPE_ID) REFERENCES TYPE (OID);
ALTER TABLE OBJECT ADD CONSTRAINT FK_OBJECT_TYPE FOREIGN KEY (TYPE_ID) REFERENCES TYPE (OID);
ALTER TABLE PROPERTY_TYPE ADD CONSTRAINT FK_PROPERTY_TYPE_TARGET_PROP FOREIGN KEY (TARGET_PROPERTY) REFERENCES PROPERTY_TYPE (OID);
ALTER TABLE OBJECT_PROPERTY ADD CONSTRAINT FK_OBJECT_PROPERTY_PROP_TYPE FOREIGN KEY (PROPERTY_TYPE) REFERENCES PROPERTY_TYPE (OID);
CREATE INDEX OBJECT_TYPE_ID ON OBJECT (TYPE_ID);
CREATE UNIQUE INDEX OBJECT_ID ON OBJECT (ID);
CREATE INDEX OBJECT_PROPERTY_VALUE_UPPER ON OBJECT_PROPERTY (UPPER(VALUE));
CREATE INDEX OBJECT_PROPERTY_OID ON OBJECT_PROPERTY (OID);
CREATE INDEX OBJECT_PROPERTY_PROP_TYPE ON OBJECT_PROPERTY (PROPERTY_TYPE);
CREATE INDEX OBJECT_PROPERTY_ID ON OBJECT_PROPERTY (ID);
CREATE INDEX OBJECT_PROPERTY_RELATED_OID ON OBJECT_PROPERTY (RELATED_OID);
CREATE INDEX OBJECT_PROPERTY_REL_PROP_TYPE ON OBJECT_PROPERTY (RELATED_PROPERTY_TYPE);
CREATE INDEX OBJECT_PROPERTY_COLINDEX ON OBJECT_PROPERTY (COLINDEX);
CREATE INDEX OBJECT_PROPERTY_VALUE ON OBJECT_PROPERTY (VALUE);
CREATE UNIQUE INDEX TYPE_TYPENAME ON TYPE (TYPENAME);
CREATE INDEX PROPERTY_TYPE_TARGET_PROPERTY ON PROPERTY_TYPE (TARGET_PROPERTY);
CREATE INDEX PROPERTY_TYPE_TYPE_ID ON PROPERTY_TYPE (TYPE_ID);
CREATE INDEX PROPERTY_TYPE_NAME ON PROPERTY_TYPE (NAME);
CREATE INDEX PROPERTY_TYPE_COLLECTION ON PROPERTY_TYPE (COLLECTION);
CREATE UNIQUE INDEX DEFAULT_OBJECT_DEF_KEY ON DEFAULT_OBJECT (DEF_KEY);
CREATE INDEX DEFAULT_OBJECT_ID ON DEFAULT_OBJECT (ID);


 -- views
 -- workspace view
 CREATE OR REPLACE VIEW workspace AS
 SELECT a.oid,
        a.id,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'name') name,
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
            AND f.name = 'URI') uri
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'description') description,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'type') type,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'nativeName') native_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'prefixedName') prefixed_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'SRS') srs,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'projectionPolicy') projection_policy,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'description') description,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'type') type,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'nativeName') native_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'prefixedName') prefixed_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'SRS') srs,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'projectionPolicy') projection_policy,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'description') description,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'capabilitiesURL') capabilities_url,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'type') type,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'nativeName') native_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'prefixedName') prefixed_name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'SRS') srs,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'projectionPolicy') projection_policy,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'filename') filename,
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'type') type,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
        (SELECT c.related_oid
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'defaultStyle.id') default_style,
        (SELECT c.related_oid
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'resource.id') "resource"
   FROM object a, type b
  WHERE a.type_id = b.oid
    AND b.typename = 'org.geoserver.catalog.LayerInfo';

 -- layergroup styles
 CREATE OR REPLACE VIEW layer_style AS
 SELECT a.oid, b.related_oid style
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'mode') "mode",
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
 SELECT a.oid, b.related_oid layer
   FROM object a, object_property b, property_type c, type d
  WHERE a.oid = b.oid
    AND a.type_id = d.oid
    AND b.property_type = c.oid
    AND c.name = 'layers.id'
    AND d.typename = 'org.geoserver.catalog.LayerGroupInfo';

 -- layergroup styles
 CREATE OR REPLACE VIEW layergroup_style AS
 SELECT a.oid, b.related_oid style
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
            AND d.name = 'featureTypeCacheSize') feature_type_cache_size,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'globalServices') global_services,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'xmlPostRequestLogBufferSize') xml_post_request_log_buf_len,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'updateSequence') update_sequence,
        (SELECT c.related_oid
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'settings.id') settings
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
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'charset') charset,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'verbose') verbose,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'verboseExceptions') verbose_exceptions,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'numDecimals') num_decimals,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'onlineResource') online_resource,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'proxyBaseUrl') proxy_base_url,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'schemaBaseUrl') schema_base_url,
        (SELECT c.related_oid
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'workspace.id') workspace
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
            AND d.name = 'name') name,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'title') title,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'abstract') abstract,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'maintainer') maintainer,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'verbose') verbose,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'citeCompliant') cite_compliant,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'outputStrategy') output_strategy,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'onlineResource') online_resource,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'schemaBaseURL') schema_base_url,
        (SELECT c.value
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'enabled') enabled,
        (SELECT c.related_oid
           FROM object_property c, property_type d
          WHERE c.oid = a.oid
            AND c.property_type = d.oid
            AND d.name = 'workspace.id') workspace
   FROM object a, type b
  WHERE a.type_id = b.oid
    AND b.typename = 'org.geoserver.config.ServiceInfo';
