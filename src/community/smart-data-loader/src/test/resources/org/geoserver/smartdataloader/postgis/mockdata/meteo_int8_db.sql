SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

DROP SCHEMA IF EXISTS smartappschematest CASCADE;
CREATE SCHEMA smartappschematest;

ALTER SCHEMA smartappschematest OWNER TO postgres;

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA smartappschematest;


COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';
SET search_path = smartappschematest, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;

CREATE TABLE smartappschematest.meteo_observations (id integer NOT NULL,parameter_id integer NOT NULL,station_id integer NOT NULL,"time" timestamp without time zone,value double PRECISION, decimal_value float4);
ALTER TABLE smartappschematest.meteo_observations OWNER TO postgres;
CREATE SEQUENCE meteo_observations_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE smartappschematest.meteo_observations_id_seq OWNER TO postgres;
ALTER SEQUENCE meteo_observations_id_seq OWNED BY meteo_observations.id;
CREATE TABLE smartappschematest.meteo_parameters (id integer NOT NULL,param_name character varying(50),param_unit character varying(10));
ALTER TABLE smartappschematest.meteo_parameters OWNER TO postgres;
CREATE TABLE smartappschematest.meteo_stations (id integer NOT NULL,code character varying(3),common_name character varying(50),"position" public.geometry(Point,4326));
ALTER TABLE smartappschematest.meteo_stations OWNER TO postgres;
ALTER TABLE ONLY smartappschematest.meteo_observations ALTER COLUMN id SET DEFAULT nextval('meteo_observations_id_seq'::regclass);
CREATE TABLE smartappschematest.meteo_maintainers (id integer NOT NULL, name character varying(50), surname character varying(50), company character varying(50), active boolean, "place" public.geography(POINT,4326), code_number int8);
ALTER TABLE smartappschematest.meteo_maintainers OWNER TO postgres;
CREATE TABLE smartappschematest.meteo_stations_maintainers (id integer NOT NULL, station_id integer, manteiner_id integer);
ALTER TABLE smartappschematest.meteo_stations_maintainers OWNER TO postgres;

INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (1,1,13,'2016-12-19 06:26:40',20, 20.3);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (2,2,13,'2016-12-19 06:27:13',155, 155.4);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (3,1,7,'2016-12-19 06:28:31',35, 31.1);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (4,1,7,'2016-12-19 06:28:55',25, 25.3);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (5,2,7,'2016-12-19 06:29:24',80, 80.4);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (6,3,7,'2016-12-19 06:30:26',1019, 1019.3);
INSERT INTO smartappschematest.meteo_observations (id, parameter_id, station_id, "time", value, decimal_value) VALUES (7,3,7,'2016-12-19 06:30:51',1015, 1015.2);

SELECT pg_catalog.setval('meteo_observations_id_seq', 7, true);

INSERT INTO smartappschematest.meteo_parameters (id, param_name, param_unit) VALUES (1,'temperature','C');
INSERT INTO smartappschematest.meteo_parameters (id, param_name, param_unit) VALUES (2,'wind speed','Km/h');
INSERT INTO smartappschematest.meteo_parameters (id, param_name, param_unit) VALUES (3,'pressure','hPa');

INSERT INTO smartappschematest.meteo_stations (id, code, common_name, "position") VALUES (13,'ALS','Alessandria','0101000020E6100000C3F5285C8F422140F6285C8FC2754640');
INSERT INTO smartappschematest.meteo_stations (id, code, common_name, "position") VALUES (7,'BOL','Bologna','0101000020E6100000AE47E17A14AE26400000000000404640');
INSERT INTO smartappschematest.meteo_stations (id, code, common_name, "position") VALUES (21,'ROV','Rovereto','0101000020E61000009A9999999919264052B81E85EBF14640');

INSERT INTO smartappschematest.meteo_maintainers (id, name, surname, company, active,"place", code_number) VALUES (1,'Franco','Migliorini','SRS srl', false, 'SRID=4326;POINT(-118.4079 33.9434)', 1);
INSERT INTO smartappschematest.meteo_maintainers (id, name, surname, company, active,"place", code_number) VALUES (2,'Alberto','Rossi','SRS srl', true, 'SRID=4326;POINT(-118.4079 33.9434)', 2);
INSERT INTO smartappschematest.meteo_maintainers (id, name, surname, company, active,"place", code_number) VALUES (3,'Mario','Bianchi','CYS srl', true,'SRID=4326;POINT(-118.4079 33.9434)', 3);

INSERT INTO smartappschematest.meteo_stations_maintainers (id, station_id, manteiner_id) VALUES (1,13,1);
INSERT INTO smartappschematest.meteo_stations_maintainers (id, station_id, manteiner_id) VALUES (2,13,2);
INSERT INTO smartappschematest.meteo_stations_maintainers (id, station_id, manteiner_id) VALUES (3,7,2);
INSERT INTO smartappschematest.meteo_stations_maintainers (id, station_id, manteiner_id) VALUES (4,7,3);
INSERT INTO smartappschematest.meteo_stations_maintainers (id, station_id, manteiner_id) VALUES (5,21,3);


ALTER TABLE ONLY smartappschematest.meteo_observations ADD CONSTRAINT meteo_observations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY smartappschematest.meteo_parameters ADD CONSTRAINT meteo_parameters_pkey PRIMARY KEY (id);
ALTER TABLE ONLY smartappschematest.meteo_stations ADD CONSTRAINT meteo_stations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY smartappschematest.meteo_observations ADD CONSTRAINT fk_parameter FOREIGN KEY (parameter_id) REFERENCES smartappschematest.meteo_parameters(id);
ALTER TABLE ONLY smartappschematest.meteo_observations ADD CONSTRAINT fk_station FOREIGN KEY (station_id) REFERENCES smartappschematest.meteo_stations(id);
ALTER TABLE ONLY smartappschematest.meteo_maintainers ADD CONSTRAINT meteo_manteneirs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY smartappschematest.meteo_stations_maintainers ADD CONSTRAINT meteo_stations_manteneirs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY smartappschematest.meteo_stations_maintainers ADD CONSTRAINT fk_stations_rel FOREIGN KEY (station_id) REFERENCES smartappschematest.meteo_stations(id);
ALTER TABLE ONLY smartappschematest.meteo_stations_maintainers ADD CONSTRAINT fk_maintainers_rel FOREIGN KEY (manteiner_id) REFERENCES smartappschematest.meteo_maintainers(id);