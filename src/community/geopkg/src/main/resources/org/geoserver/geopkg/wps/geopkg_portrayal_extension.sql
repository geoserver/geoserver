CREATE TABLE 'gpkgext_styles' (
  id INTEGER PRIMARY KEY AUTOINCREMENT, 
  style TEXT NOT NULL, 
  description TEXT, 
  uri TEXT NOT NULL
);
CREATE TABLE 'gpkgext_symbols' (
  id INTEGER PRIMARY KEY AUTOINCREMENT, 
  symbol TEXT NOT NULL, 
  description TEXT, 
  uri TEXT NOT NULL
);
CREATE TABLE 'gpkgext_symbol_content' (
  id INTEGER PRIMARY KEY AUTOINCREMENT, 
  format TEXT NOT NULL, 
  content BLOB NOT NULL, 
  uri TEXT NOT NULL
);
CREATE TABLE 'gpkgext_symbol_images' (
  id INTEGER PRIMARY KEY AUTOINCREMENT, 
  symbol_id INTEGER NOT NULL, 
  content_id INTEGER NOT NULL, 
  width INTEGER, 
  height INTEGER, 
  offset_x INTEGER, 
  offset_y INTEGER, 
  pixel_ratio INTEGER, 
  CONSTRAINT symbol_id_fk FOREIGN KEY (symbol_id) REFERENCES gpkgext_symbols(id), 
  CONSTRAINT content_id_fk FOREIGN KEY (content_id) REFERENCES gpkgext_symbol_content(id)
);
CREATE TABLE 'gpkgext_stylesheets' (
  id INTEGER PRIMARY KEY AUTOINCREMENT, 
  style_id INTEGER NOT NULL, 
  format TEXT NOT NULL, 
  stylesheet BLOB NOT NULL, 
  CONSTRAINT style_id_fk FOREIGN KEY (style_id) REFERENCES gpkgext_styles(id)
);
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_styles', null, 'im_portrayal', 
    'https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/5-portrayal.adoc', 
    'read-write'
  );
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_symbols', null, 'im_portrayal', 
    'https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/5-portrayal.adoc', 
    'read-write'
  );
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_stylesheets', null, 'im_portrayal', 
    'https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/5-portrayal.adoc', 
    'read-write'
  );
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_symbol_content', null, 'im_portrayal', 
    'https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/5-portrayal.adoc', 
    'read-write'
  );
INSERT INTO gpkg_extensions 
VALUES 
  (
    'gpkgext_symbol_images', null, 'im_portrayal', 
    'https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/5-portrayal.adoc', 
    'read-write'
  );
