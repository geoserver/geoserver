-- granule data, fake, assuming 1 granule per feature just for the sake of testing
INSERT INTO granule
(product_id, location, the_geom)
SELECT "id", '/efs/geoserver_data/coverages/sentinel/california/' || 'eoIdentifier' || '.tif', "footprint" 
FROM product;
