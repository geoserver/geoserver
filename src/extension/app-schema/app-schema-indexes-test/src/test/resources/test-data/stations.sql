
SET row_security = off;

DROP SCHEMA IF EXISTS meteo CASCADE;
CREATE SCHEMA meteo;

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

SET search_path = meteo, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

CREATE TABLE meteo_observations (
    id serial NOT NULL,
    parameter_id integer NOT NULL,
    station_id integer NOT NULL,
    "time" timestamp without time zone,
    value double precision,
    description text
);

CREATE TABLE meteo_observations_comments (
    id integer NOT NULL,
    observation_id integer NOT NULL,
    comment character varying(1000) NOT NULL
);

CREATE TABLE meteo_observations_tags (
    id integer NOT NULL,
    observation_id integer NOT NULL,
    tag character varying(50) NOT NULL
);

CREATE TABLE meteo_parameters (
    id integer NOT NULL,
    param_name character varying(50),
    param_unit character varying(10)
);

CREATE TABLE meteo_stations (
    id integer NOT NULL,
    code character varying(3),
    common_name character varying(50),
    "position" public.geometry(Point,4326),
    comments text
);

CREATE TABLE meteo_stations_comments (
    id integer NOT NULL,
    station_id integer NOT NULL,
    comment character varying(100) NOT NULL
);

CREATE TABLE meteo_stations_tags (
    id integer NOT NULL,
    station_id integer NOT NULL,
    tag character varying(50) NOT NULL
);

INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (5, 2, 7, '2016-12-19 06:29:24', 80, NULL);
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (6, 3, 7, '2016-12-19 06:30:26', 1019, NULL);
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (7, 3, 7, '2016-12-19 06:30:51', 1015, NULL);
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (4, 1, 7, '2016-12-19 06:28:55', 25, 'precipitation on a routine basis');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (2, 2, 13, '2016-12-19 06:27:13', 155, 'We know that some users may have experienced difficulty accessing OneDrive for Business on Linux');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (1, 1, 13, '2016-12-19 06:26:40', 20, 'wrapper');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (3, 1, 7, '2016-12-19 06:28:31', 35, 'index working');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (9, 2, 21, '2018-12-19 06:30:51', 2, 'wing it');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (10, 1, 22, '2018-12-19 06:30:51', 34, 'past verbs');
INSERT INTO meteo_observations (id, parameter_id, station_id, "time", value, description) VALUES (8, 2, 21, '2018-12-19 06:30:51', 70, 'its bad testing on production');

INSERT INTO meteo_observations_comments (id, observation_id, comment) VALUES (1, 1, 'comment1_ALS');
INSERT INTO meteo_observations_comments (id, observation_id, comment) VALUES (2, 1, 'comment2_ALS');
INSERT INTO meteo_observations_comments (id, observation_id, comment) VALUES (3, 2, 'comment1_BOL');
INSERT INTO meteo_observations_comments (id, observation_id, comment) VALUES (4, 3, 'comment1_BOL');
INSERT INTO meteo_observations_comments (id, observation_id, comment) VALUES (5, 3, 'comment2_BOL');

INSERT INTO meteo_observations_tags (id, observation_id, tag) VALUES (1, 1, 'tag1_ALS');
INSERT INTO meteo_observations_tags (id, observation_id, tag) VALUES (2, 1, 'tag2_ALS');
INSERT INTO meteo_observations_tags (id, observation_id, tag) VALUES (3, 2, 'tag1_BOL');
INSERT INTO meteo_observations_tags (id, observation_id, tag) VALUES (4, 3, 'tag1_BOL');
INSERT INTO meteo_observations_tags (id, observation_id, tag) VALUES (5, 3, 'tag2_BOL');

INSERT INTO meteo_parameters (id, param_name, param_unit) VALUES (1, 'temperature', 'C');
INSERT INTO meteo_parameters (id, param_name, param_unit) VALUES (2, 'wind speed', 'Km/h');
INSERT INTO meteo_parameters (id, param_name, param_unit) VALUES (3, 'pressure', 'hPa');

INSERT INTO meteo_stations (id, code, common_name, "position", comments) VALUES (7, 'BOL', 'Bologna', '0101000020E6100000AE47E17A14AE26400000000000404640', 'The probability of precipitation (POP), is defined as the likelihood of occurrence (expressed as a percent) of a measurable amount of liquid precipitation');
INSERT INTO meteo_stations (id, code, common_name, "position", comments) VALUES (13, 'ALS', 'Alessandria', '0101000020E6100000C3F5285C8F422140F6285C8FC2754640', 'No risk of severe thunders');
INSERT INTO meteo_stations (id, code, common_name, "position", comments) VALUES (21, 'ROV', 'Rovereto2', '0101000020E61000009A9999999919264052B81E85EBF14640', 'Kennedy North');
INSERT INTO meteo_stations (id, code, common_name, "position", comments) VALUES (22, 'LON', 'London', '0101000020E61000009A9999999919264052B81E85EBF14640', 'Park avenue');

INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (1, 7, 'comment1_BOL');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (2, 7, 'comment2_BOL');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (3, 7, 'comment3_BOL');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (4, 7, 'comment4_BOL');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (5, 13, 'comment1_ALS');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (6, 13, 'comment2_ALS');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (7, 13, 'comment3_ALS');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (8, 13, 'comment4_ALS');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (9, 21, 'comment5_ROV');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (10, 21, 'comment1_ROV');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (11, 21, 'comment2_ROV');
INSERT INTO meteo_stations_comments (id, station_id, comment) VALUES (12, 21, 'comment3_ROV');


INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (1, 7, 'tag1_BOL');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (2, 7, 'tag2_BOL');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (3, 7, 'tag3_BOL');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (4, 7, 'tag4_BOL');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (5, 13, 'tag1_ALS');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (6, 13, 'tag2_ALS');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (7, 13, 'tag3_ALS');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (8, 13, 'tag4_ALS');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (9, 21, 'tag5_ROV');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (10, 21, 'tag1_ROV');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (11, 21, 'tag2_ROV');
INSERT INTO meteo_stations_tags (id, station_id, tag) VALUES (12, 21, 'tag3_ROV');


ALTER TABLE ONLY meteo_observations_comments
    ADD CONSTRAINT meteo_observations_comments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_observations
    ADD CONSTRAINT meteo_observations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_observations_tags
    ADD CONSTRAINT meteo_observations_tags_pkey PRIMARY KEY (id);


ALTER TABLE ONLY meteo_parameters
    ADD CONSTRAINT meteo_parameters_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_stations_comments
    ADD CONSTRAINT meteo_stations_comments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_stations
    ADD CONSTRAINT meteo_stations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_stations_tags
    ADD CONSTRAINT meteo_stations_tags_pkey PRIMARY KEY (id);

ALTER TABLE ONLY meteo_observations_comments
    ADD CONSTRAINT fk_observation_comment FOREIGN KEY (observation_id) REFERENCES meteo_observations(id);

ALTER TABLE ONLY meteo_observations_tags
    ADD CONSTRAINT fk_observation_tag FOREIGN KEY (observation_id) REFERENCES meteo_observations(id);

ALTER TABLE ONLY meteo_observations
    ADD CONSTRAINT fk_parameter FOREIGN KEY (parameter_id) REFERENCES meteo_parameters(id);

ALTER TABLE ONLY meteo_observations
    ADD CONSTRAINT fk_station FOREIGN KEY (station_id) REFERENCES meteo_stations(id);

ALTER TABLE ONLY meteo_stations_comments
    ADD CONSTRAINT fk_station_comment FOREIGN KEY (station_id) REFERENCES meteo_stations(id);

ALTER TABLE ONLY meteo_stations_tags
    ADD CONSTRAINT fk_station_tag FOREIGN KEY (station_id) REFERENCES meteo_stations(id);



