-- granule data, fake, assuming 1 granule per feature just for the sake of testing
INSERT INTO granule
("product_id", "location", "the_geom")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint" 
FROM product where "eoParentIdentifier" <> 'SENTINEL2';
-- SENTINEL2 is supposed to be a split multiband case (H2 odes not support any array expansion to do this in a single query...)
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint", 'B01'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint", 'B02'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint", 'B03'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint", 'B04'
FROM product where "eoParentIdentifier" = 'SENTINEL2';


