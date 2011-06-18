-- PostGIS setup script for wfs 1.1 sf0 cite dataset
-- Usage:
--     psql -U <database owner> <database> < dataset-sf0.sql
SET client_encoding = 'UTF8';

delete from geometry_columns where f_table_name = 'PrimitiveGeoFeature' ;
delete from geometry_columns where f_table_name = 'AggregateGeoFeature' ;
delete from geometry_columns where f_table_name = 'EntitéGénérique' ;

drop table "PrimitiveGeoFeature";
create table "PrimitiveGeoFeature" ( description varchar, name varchar );

select addgeometrycolumn( 'public', 'PrimitiveGeoFeature', 'surfaceProperty', 4326, 'POLYGON', 2 );
select addgeometrycolumn( 'public', 'PrimitiveGeoFeature', 'pointProperty', 4326, 'POINT', 2 );
select addgeometrycolumn( 'public', 'PrimitiveGeoFeature', 'curveProperty', 4326, 'LINESTRING', 2 );

alter table "PrimitiveGeoFeature" add  "intProperty" int not null;
alter table "PrimitiveGeoFeature" add  "uriProperty" varchar;
alter table "PrimitiveGeoFeature" add measurand float not null;
alter table "PrimitiveGeoFeature" add "dateTimeProperty" timestamp with time zone;
--alter table "PrimitiveGeoFeature" add "dateProperty" date;
alter table "PrimitiveGeoFeature" add "dateProperty" timestamp with time zone;
alter table "PrimitiveGeoFeature" add "decimalProperty" float not null;
alter table "PrimitiveGeoFeature" add id varchar; 
alter table "PrimitiveGeoFeature" add primary key ( id );

INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f001', 'name-f001', NULL, geometryfromtext('POINT(2.00342 39.73245)',4326), NULL, 155, 'http://www.opengeospatial.org/', 12765, NULL, '2006-10-25Z', 5.03, 'f001');
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f002', 'name-f002', NULL, geometryfromtext('POINT(0.22601 59.41276)',4326), NULL, 154, 'http://www.opengeospatial.org/', 12769, NULL, '2006-10-23Z', 4.02, 'f002');
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f003', 'name-f003',  NULL, NULL, geometryfromtext('LINESTRING(9.799 46.074,10.466 46.652,11.021 47.114)',4326) , 180, NULL, 672.1, NULL, '2006-09-01Z', 12.92, 'f003');
INSERT INTO "PrimitiveGeoFeature" VALUES ('description-f008', 'name-f008',  geometryfromtext('POLYGON((30.899 45.174,30.466 45.652,30.466 45.891,30.899 45.174))',4326) , NULL, NULL, 300, NULL, 783.5, '2006-06-28T07:08:00+02:00', '2006-12-12Z', 18.92, 'f008');
INSERT INTO "PrimitiveGeoFeature" VALUES (NULL, 'name-f015',  NULL, geometryfromtext('POINT(-10.52 34.94)',4326), NULL, -900, NULL, 2.4, NULL, NULL, 7.90, 'f015');


drop table "AggregateGeoFeature";
create table "AggregateGeoFeature" ( description varchar, name varchar );

select addgeometrycolumn( 'public', 'AggregateGeoFeature', 'multiPointProperty', 4326, 'MULTIPOINT', 2 );
select addgeometrycolumn( 'public', 'AggregateGeoFeature', 'multiCurveProperty', 4326, 'MULTILINESTRING', 2 );
select addgeometrycolumn( 'public', 'AggregateGeoFeature', 'multiSurfaceProperty', 4326, 'MULTIPOLYGON', 2 );

alter table "AggregateGeoFeature" add  "doubleProperty" float not null;
alter table "AggregateGeoFeature" add  "intRangeProperty" varchar;
alter table "AggregateGeoFeature" add  "strProperty" varchar not null;
alter table "AggregateGeoFeature" add  "featureCode" varchar not null;
alter table "AggregateGeoFeature" add id varchar; 
alter table "AggregateGeoFeature" add primary key ( id );

INSERT INTO "AggregateGeoFeature" VALUES ('description-f005','name-f005',geometryfromtext('MULTIPOINT((29.86 70.83),(31.08 68.87),(32.19 71.96))',4326),NULL,NULL,2012.78,NULL,'Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari quam li existent Europan lingues.','BK030','f005');
INSERT INTO "AggregateGeoFeature" VALUES ('description-f009','name-f009',NULL,geometryfromtext('MULTILINESTRING((-5.899 55.174,-5.466 55.652,-5.899 55.891,-5.899 58.174,-5.466 58.652,-5.899 58.891),(-5.188 53.265,-4.775 54.354,-4.288 52.702,-4.107 53.611,-4.010 55.823))',4326),NULL,20.01,NULL,'Ma quande lingues coalesce, li grammatica del resultant.','GB007','f009');
INSERT INTO "AggregateGeoFeature" VALUES ('description-f010','name-f010',NULL,NULL,geometryfromtext('MULTIPOLYGON(((20 50,19 54,20 55,30 60,28 52,27 51,29 49,27 47,20 50),(25 55,25.2 56,25.1 56,25 55)),((20.0 35.5,24.0 35.0,28.0 35.0,27.5 39.0,22.0 37.0,20.0 35.5),(26.0 36.0,25.0 37.0,27.0 36.8,26.0 36.0)))',4326),24510,NULL,' Ma quande lingues coalesce, li grammatica del resultant lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua franca va esser plu simplic e regulari quam li existent Europan lingues.','AK020','f010');
INSERT INTO "AggregateGeoFeature" VALUES (NULL,'name-f016',NULL,NULL,geometryfromtext('MULTIPOLYGON(((6.0 57.5, 8.0 57.5, 8.0 60.0, 9.0 62.5, 5.0 62.5,6.0 60.0,6.0 57.5),(6.5 58.0,6.5 59.0,7.0 59.0,6.5 58.0)))',4326),-182.9,NULL,'In rhoncus nisl sit amet sem.','EE010','f016');


drop table "EntitéGénérique";
create table "EntitéGénérique" ( description varchar, name varchar );

select addgeometrycolumn( 'public', 'EntitéGénérique', 'attribut.Géométrie', 4326, 'GEOMETRY', 2 );

alter table "EntitéGénérique" add  "boolProperty" boolean not null;
alter table "EntitéGénérique" add  "str4Property" varchar not null;
alter table "EntitéGénérique" add  "featureRef" varchar;
alter table "EntitéGénérique" add id varchar; 
alter table "EntitéGénérique" add primary key ( id );

INSERT INTO "EntitéGénérique" VALUES ('description-f004','name-f004',geometryfromtext('POLYGON((0 60.5,0 64,6.25 64,6.25 60.5,0 60.5),(2 61.5,2 62.5,4 62,2 61.5))',4326),true,'abc3','name-f003', 'f004');
INSERT INTO "EntitéGénérique" VALUES ('description-f007','name-f007',geometryfromtext('POLYGON((15 35,16 40,20 39,22.5 37,18 36,15 35),(17.5 37.1,17.6 37.2,17.7 37.3,17.8 37.4,17.9 37.5,17.9 37,17.5 37.1))',4326),false,'def4',NULL,'f007');
INSERT INTO "EntitéGénérique" VALUES ('description-f017','name-f017',geometryfromtext('LINESTRING(4.899 50.174,5.466 52.652,6.899 53.891,7.780 54.382,8.879 54.982)',4326),false,'qrst','name-f015','f017');
