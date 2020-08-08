create table gpkgext_generalized (
  primary_table text references gpkg_contents(table_name) NOT NULL,
  generalized_table text NOT NULL,
  distance real NOT NULL,
  provenance text,
  unique(primary_table, generalized_table),
  check (distance > 0)
);
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_generalized', null, 'tb16_generalized', 
    'https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/tree/master/ER', 
    'read-write'
  );
