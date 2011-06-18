-- H2 setup script for wfs 1.1 sf2 cite dataset

-- PrimitiveGeoFeature
--DROP TABLE "PrimitiveGeoFeature";
CREATE TABLE "PrimitiveGeoFeature" ( "description" varchar, "name" varchar );

ALTER TABLE "PrimitiveGeoFeature" ADD "surfaceProperty" blob; 
ALTER TABLE "PrimitiveGeoFeature" ADD "pointProperty" blob; 
ALTER TABLE "PrimitiveGeoFeature" ADD "curveProperty" blob; 
ALTER TABLE "PrimitiveGeoFeature" ADD "intProperty" int8 not null;
ALTER TABLE "PrimitiveGeoFeature" ADD "uriProperty" varchar;
ALTER TABLE "PrimitiveGeoFeature" ADD "measurand" float not null;
ALTER TABLE "PrimitiveGeoFeature" ADD "dateTimeProperty" datetime;
ALTER TABLE "PrimitiveGeoFeature" ADD "dateProperty" datetime;
ALTER TABLE "PrimitiveGeoFeature" ADD "decimalProperty" double(2) not null;
ALTER TABLE "PrimitiveGeoFeature" ADD "relatedFeature" varchar; 
ALTER TABLE "PrimitiveGeoFeature" ADD "id" int AUTO_INCREMENT(1);
ALTER TABLE "PrimitiveGeoFeature" ADD CONSTRAINT "surfacePropertyGeometryType" CHECK GeometryType("surfaceProperty") IS NULL OR GeometryType("surfaceProperty") = 'POLYGON';
ALTER TABLE "PrimitiveGeoFeature" ADD CONSTRAINT "pointPropertyGeometryType" CHECK GeometryType("pointProperty") IS NULL OR GeometryType("pointProperty") = 'POINT';
ALTER TABLE "PrimitiveGeoFeature" ADD CONSTRAINT "curvePropertyGeometryType" CHECK GeometryType("curveProperty") IS NULL OR GeometryType("curveProperty") = 'LINESTRING';

-- PrimitiveGeoFeature.f001
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f001', 'name-f001', NULL,GeomFromText('POINT(2.00342 39.73245)',4326), NULL, 155, 'http://www.opengeospatial.org/', 12765, NULL, ParseDateTime('2006-10-25 GMT','yyyy-MM-dd','en','GMT'), 5.03, NULL, 1);

-- PrimitiveGeoFeature.f002
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f002', 'name-f002', NULL,GeomFromText('POINT(0.22601 59.41276)',4326), NULL, 154, 'http://www.opengeospatial.org/', 12769, NULL, ParseDateTime('2006-10-23 GMT','yyyy-MM-dd','en','GMT'), 4.02, NULL, 2);

-- PrimitiveGeoFeature.f003
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f003', 'name-f003',  NULL, NULL, GeomFromText('LINESTRING(9.799 46.074,10.466 46.652,11.021 47.114)',4326) , 180, NULL, 672.1, NULL, ParseDateTime('2006-09-01 GMT','yyyy-MM-dd','en','GMT'), 12.92, NULL, 3);

-- PrimitiveGeoFeature.f008
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f008', 'name-f008', GeomFromText('POLYGON((30.899 45.174,30.466 45.652,30.466 45.891,30.899 45.174))',4326), NULL, NULL, 300, NULL, 783.5, '2006-06-28T07:08:00+02:00', ParseDateTime('2006-12-12 GMT','yyyy-MM-dd z','en','GMT'), 18.92, NULL, 8);

-- PrimitiveGeoFeature.f015
INSERT INTO "PrimitiveGeoFeature" VALUES (NULL, 'name-f015',  NULL, GeomFromText('POINT(-10.52 34.94)',4326), NULL, -900, NULL, 2.4, NULL, NULL, 7.90, NULL, 15);

-- AggregateGeoFeature
--DROP TABLE "AggregateGeoFeature";
CREATE TABLE "AggregateGeoFeature" ( "description" varchar, "name" varchar );

ALTER TABLE "AggregateGeoFeature" ADD "multiPointProperty" blob;
ALTER TABLE "AggregateGeoFeature" ADD "multiCurveProperty" blob;
ALTER TABLE "AggregateGeoFeature" ADD "multiSurfaceProperty" blob;
ALTER TABLE "AggregateGeoFeature" ADD "doubleProperty" float not null;
ALTER TABLE "AggregateGeoFeature" ADD "intRangeProperty" varchar;
ALTER TABLE "AggregateGeoFeature" ADD "strProperty" varchar not null;
ALTER TABLE "AggregateGeoFeature" ADD "featureCode" varchar not null;
ALTER TABLE "AggregateGeoFeature" ADD "id" int AUTO_INCREMENT(1);
ALTER TABLE "AggregateGeoFeature" ADD CONSTRAINT "multiPointPropertyGeometryType" CHECK GeometryType("multiPointProperty") IS NULL OR GeometryType("multiPointProperty") = 'MULTIPOINT';
ALTER TABLE "AggregateGeoFeature" ADD CONSTRAINT "multiCurvePropertyGeometryType" CHECK GeometryType("multiCurveProperty") IS NULL OR GeometryType("multiCurveProperty") = 'MULTILINESTRING';
ALTER TABLE "AggregateGeoFeature" ADD CONSTRAINT "multiSurfacePropertyGeometryType" CHECK GeometryType("multiSurfaceProperty") IS NULL OR GeometryType("multiSurfaceProperty") = 'MULTIPOLYGON';

-- AggregateGeoFeature.f005
INSERT INTO "AggregateGeoFeature" VALUES ('description-f005','name-f005',GeomFromText('MULTIPOINT(29.86 70.83,31.08 68.87,32.19 71.96)',4326),NULL,NULL,2012.78,NULL,'Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari quam li existent Europan lingues.','BK030',5);

-- AggregateGeoFeature.f009
INSERT INTO "AggregateGeoFeature" VALUES ('description-f009','name-f009',NULL,GeomFromText('MULTILINESTRING((-5.899 55.174,-5.466 55.652,-5.899 55.891,-5.899 58.174,-5.466 58.652,-5.899 58.891),(-5.188 53.265,-4.775 54.354,-4.288 52.702,-4.107 53.611,-4.010 55.823))',4326),NULL,20.01,NULL,'Ma quande lingues coalesce, li grammatica del resultant.','GB007',9);

-- AggregateGeoFeature.f010
INSERT INTO "AggregateGeoFeature" VALUES ('description-f010','name-f010',NULL,NULL,GeomFromText('MULTIPOLYGON(((20 50,19 54,20 55,30 60,28 52,27 51,29 49,27 47,20 50),(25 55,25.2 56,25.1 56,25 55)),((20.0 35.5,24.0 35.0,28.0 35.0,27.5 39.0,22.0 37.0,20.0 35.5),(26.0 36.0,25.0 37.0,27.0 36.8,26.0 36.0)))',4326),24510,NULL,' Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari quam li existent Europan lingues.','AK020',10);

-- AggregateGeoFeature.f016
INSERT INTO "AggregateGeoFeature" VALUES (NULL,'name-f016',NULL,NULL,GeomFromText('MULTIPOLYGON(((6.0 57.5, 8.0 57.5, 8.0 60.0, 9.0 62.5, 5.0 62.5,6.0 60.0,6.0 57.5),(6.5 58.0,6.5 59.0,7.0 59.0,6.5 58.0)))',4326),-182.9,NULL,'In rhoncus nisl sit amet sem.','EE010',16);

-- EntitéGénérique 
--DROP TABLE "EntitéGénérique";
CREATE TABLE "EntitéGénérique" ( "description" varchar, "name" varchar );

ALTER TABLE "EntitéGénérique" ADD "attribut.Géométrie" blob;
ALTER TABLE "EntitéGénérique" ADD "boolProperty" boolean not null;
ALTER TABLE "EntitéGénérique" ADD "str4Property" varchar not null;
ALTER TABLE "EntitéGénérique" ADD "featureRef" varchar;
ALTER TABLE "EntitéGénérique" ADD "id" int AUTO_INCREMENT(1);
--ALTER TABLE "EntitéGénérique" ADD "id" varchar not null; 
--ALTER TABLE "EntitéGénérique" ADD primary key ( "id" );

-- EntitéGénérique.f004
INSERT INTO "EntitéGénérique" VALUES ('description-f004','name-f004',GeomFromText('POLYGON((0 60.5,0 64,6.25 64,6.25 60.5,0 60.5),(2 61.5,2 62.5,4 62,2 61.5))',4326),true,'abc3','name-f003', 4);

-- EntitéGénérique.f007
INSERT INTO "EntitéGénérique" VALUES ('description-f007','name-f007',GeomFromText('POLYGON((15 35,16 40,20 39,22.5 37,18 36,15 35),(17.5 37.1,17.6 37.2,17.7 37.3,17.8 37.4,17.9 37.5,17.9 37,17.5 37.1))',4326),false,'def4',NULL,7);

-- EntitéGénérique.f017
INSERT INTO "EntitéGénérique" VALUES ('description-f017','name-f017',GeomFromText('LINESTRING(4.899 50.174,5.466 52.652,6.899 53.891,7.780 54.382,8.879 54.982)',4326),false,'qrst','name-f015',17);
