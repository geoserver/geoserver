ALTER TABLE product ADD COLUMN "gsTest" VARCHAR DEFAULT 'abc';
INSERT INTO collection("id", "name", "footprint", "timeStart", "timeEnd", "eoIdentifier", "eoSensorType")
       VALUES(33, 'GS_TEST', ST_GeomFromText('POLYGON((-179 89,179 89,179 -89,-179 -89,-179 89))', 4326), 
              '2015-06-01 10:20:21.000', '2016-02-26 10:20:21.000', 'gsTestCollection', 'geoServer');
commit ;
INSERT INTO product ("id", "timeStart", "timeEnd", "eoIdentifier", "eoParentIdentifier", "footprint", "gsTest")
       VALUES(372, '2017-02-26 10:20:21.026', '2017-02-26 10:20:21.026', 'GS_TEST_PRODUCT.01', 'gsTestCollection',
       ST_GeomFromText('POLYGON((11.5838082795702 43.2356009596972,11.6266459737 44.2232097399754,10.2524808722177 44.2465509344875,10.2320327000367 43.2581553186329,11.5838082795702 43.2356009596972))', 4326),
       'abc');
 