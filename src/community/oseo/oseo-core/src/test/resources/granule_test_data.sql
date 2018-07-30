-- granule data, fake, assuming 1 granule per feature just for the sake of testing
INSERT INTO granule
("product_id", "location", "the_geom")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint"::geometry
FROM product where "eoParentIdentifier" <> 'SENTINEL2';
-- SENTINEL2 is supposed to be a split multiband case (H2 odes not support any array expansion to do this in a single query...)
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint"::geometry, 'B01'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint"::geometry, 'B02'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint"::geometry, 'B03'
FROM product where "eoParentIdentifier" = 'SENTINEL2';
INSERT INTO granule
("product_id", "location", "the_geom", "band")
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || "eoIdentifier" || '.tif', "footprint"::geometry, 'B04'
FROM product where "eoParentIdentifier" = 'SENTINEL2';


