-- This file inserts all the appropriate cite data into postgis to run with
-- geoserver.  To work with the tar file of featureType directories it
-- should be run in a database called cite, that either has a user with
-- a password of cite, cite, or that allows anyone read/write access.
-- You can also just run this script in whatever database you like with
-- your user name, but then you have to modify all the user access info
-- of the info.xml files.  Uncommenting lines at 253 will create the cite
-- user and grant access to it on all the relavant tables, the user you are
-- connecting with must have the appropriate permissions to do that.  


-- Uncomment and change this to the user who has permissions to drop and
-- create tables in this db.
\connect cite_wfs10 cite

--create extension postgis;


--uncomment these if you want to reset everything.
--drop table "Nulls";
--drop table "Points";
--drop table "Other";
--drop table "Lines";
--drop table "Polygons";
--drop table "MLines";
--drop table "MPolygons";
--drop table "MPoints";
--drop table "Seven";
--drop table "Fifteen";
--drop table "Updates";
--drop table "Inserts";
--drop table "Deletes";
--drop table "Locks";
--delete from "geometry_columns" where srid=32615;




--
-- TOC Entry ID 23 (OID 312261)
--
-- Name: SevenFeature Type: TABLE Owner: ciesin
--

CREATE TABLE "Seven" (
	"boundedBy" geometry('POLYGON', 32615),
	"pointProperty" geometry('POINT', 32615)
);


--
-- TOC Entry ID 24 (OID 312275)
--
-- Name: NullFeature Type: TABLE Owner: cite
--

CREATE TABLE "Nulls" (
	"description" character varying,
	"name" character varying,
	"boundedBy" geometry('POLYGON', 32615),
	"integers" integer,
	"dates" date,
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 25 (OID 312300)
--
-- Name: DeleteFeature Type: TABLE Owner: cite
--

CREATE TABLE "Deletes" (
	"boundedBy" geometry('POLYGON', 32615),
	"id" character varying,
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 26 (OID 312305)
--
-- Name: InsertFeature Type: TABLE Owner: cite
--

CREATE TABLE "Inserts" (
	"boundedBy" geometry('POLYGON', 32615),
	"id" character varying,
	"pointProperty" geometry('POINT', 32615)
);


--
-- TOC Entry ID 27 (OID 312310)
--
-- Name: UpdateFeature Type: TABLE Owner: cite
--

CREATE TABLE "Updates" (
	"boundedBy" geometry('POLYGON', 32615),
	"id" character varying,
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 28 (OID 312315)
--
-- Name: PointFeature Type: TABLE Owner: cite
--

CREATE TABLE "Points" (
	"id" character varying,
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 29 (OID 312322)
--
-- Name: LineStringFeature Type: TABLE Owner: cite
--

CREATE TABLE "Lines" (
	"id" character varying,
	"lineStringProperty" geometry('LINESTRING', 32615)
);

--
-- TOC Entry ID 30 (OID 312329)
--
-- Name: PolygonFeature Type: TABLE Owner: cite
--

CREATE TABLE "Polygons" (
	"id" character varying,
	"polygonProperty" geometry('POLYGON', 32615)
);

--
-- TOC Entry ID 31 (OID 312335)
--
-- Name: MultiPointFeature Type: TABLE Owner: cite
--

CREATE TABLE "MPoints" (
	"id" character varying,
	"multiPointProperty" geometry('MULTIPOINT', 32615)
);

--
-- TOC Entry ID 32 (OID 312341)
--
-- Name: MultiLineStringFeature Type: TABLE Owner: cite
--

CREATE TABLE "MLines" (
	"id" character varying,
	"multiLineStringProperty" geometry('MULTILINESTRING', 32615)
);

--
-- TOC Entry ID 33 (OID 312348)
--
-- Name: MultiPolygonFeature Type: TABLE Owner: cite
--

CREATE TABLE "MPolygons" (
	"id" character varying,
	"multiPolygonProperty" geometry('MULTIPOLYGON', 32615)
);

--
-- TOC Entry ID 34 (OID 312391)
--
-- Name: FifteenFeature Type: TABLE Owner: cite
--

CREATE TABLE "Fifteen" (
	"boundedBy" geometry('POLYGON', 32615),
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 35 (OID 312430)
--
-- Name: LockFeature Type: TABLE Owner: cite
--

CREATE TABLE "Locks" (
	"boundedBy" geometry('POLYGON', 32615),
	"id" character varying, 
	"pointProperty" geometry('POINT', 32615)
);

--
-- TOC Entry ID 36 (OID 312570)
--
-- Name: OtherFeature Type: TABLE Owner: cite
--

CREATE TABLE "Other" (
	"description" character varying,
	"name" character varying,
	"boundedBy" geometry('POLYGON', 32615),
	"pointProperty" geometry('POINT', 32615),
	"string1" character varying NOT NULL,
	"string2" character varying,
	"integers" integer,
	"dates" date
);

--
-- Data for TOC Entry ID 39 (OID 113496)
--
-- Name: county Type: TABLE DATA Owner: public
--


 

COPY "Seven" FROM stdin;
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
\.
--
-- Data for TOC Entry ID 59 (OID 312275)
--
-- Name: NullFeature Type: TABLE DATA Owner: public
--


COPY "Nulls" FROM stdin;
nullFeature	\N	SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	\N	\N	\N
\.
--
-- Data for TOC Entry ID 60 (OID 312300)
--
-- Name: DeleteFeature Type: TABLE DATA Owner: public
--


COPY "Deletes" FROM stdin;
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	td0001	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	td0002	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	td0003	SRID=32615;POINT(500050 500050)
\.
--
-- Data for TOC Entry ID 61 (OID 312305)
--
-- Name: InsertFeature Type: TABLE DATA Owner: public
--


COPY "Inserts" FROM stdin;
\.
--
-- Data for TOC Entry ID 62 (OID 312310)
--
-- Name: UpdateFeature Type: TABLE DATA Owner: public
--


COPY "Updates" FROM stdin;
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	tu0001	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	tu0002	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	tu0003	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	tu0004	SRID=32615;POINT(500050 500050)
\.
--
-- Data for TOC Entry ID 63 (OID 312315)
--
-- Name: PointFeature Type: TABLE DATA Owner: public
--


COPY "Points" FROM stdin;
t0000	SRID=32615;POINT(500050 500050)
\.
--
-- Data for TOC Entry ID 64 (OID 312322)
--
-- Name: LineStringFeature Type: TABLE DATA Owner: public
--


COPY "Lines" FROM stdin;
t0001	SRID=32615;LINESTRING(500125 500025,500175 500075)
\.
--
-- Data for TOC Entry ID 65 (OID 312329)
--
-- Name: PolygonFeature Type: TABLE DATA Owner: public
--


COPY "Polygons" FROM stdin;
t0002	SRID=32615;POLYGON((500225 500025,500225 500075,500275 500050,500275 500025,500225 500025))
\.
--
-- Data for TOC Entry ID 66 (OID 312335)
--
-- Name: MultiPointFeature Type: TABLE DATA Owner: public
--


COPY "MPoints" FROM stdin;
t0003	SRID=32615;MULTIPOINT(500325 500025,500375 500075)
\.
--
-- Data for TOC Entry ID 67 (OID 312341)
--
-- Name: MultiLineStringFeature Type: TABLE DATA Owner: public
--


COPY "MLines" FROM stdin;
t0004	SRID=32615;MULTILINESTRING((500425 500025,500475 500075),(500425 500075,500475 500025))
\.
--
-- Data for TOC Entry ID 68 (OID 312348)
--
-- Name: MultiPolygonFeature Type: TABLE DATA Owner: public
--


COPY "MPolygons" FROM stdin;
t0005	SRID=32615;MULTIPOLYGON(((500525 500025,500550 500050,500575 500025,500525 500025)),((500525 500050,500525 500075,500550 500075,500550 500050,500525 500050)))
\.
--
-- Data for TOC Entry ID 69 (OID 312391)
--
-- Name: FifteenFeature Type: TABLE DATA Owner: public
--


COPY "Fifteen" FROM stdin;
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)
\.
--
-- Data for TOC Entry ID 70 (OID 312430)
--
-- Name: LockFeature Type: TABLE DATA Owner: public
--


COPY "Locks" FROM stdin;
\N	lfla0001	\N
\N	lfla0002	\N
\N	lfla0003	\N
\N	lfla0004	\N
\N	gfwlla0001	\N
\N	gfwlla0002	\N
\N	gfwlla0003	\N
\N	gfwlla0004	\N
\N	lfbt0001	\N
\N	lfbt0002	\N
\N	lfbt0003	\N
\N	lfbt0004	\N
\N	lfbt0005	\N
\N	lfbt0006	\N
\N	gfwlbt0001	\N
\N	gfwlbt0002	\N
\N	gfwlbt0003	\N
\N	gfwlbt0004	\N
\N	gfwlbt0005	\N
\N	gfwlbt0006	\N
\N	lfe0001	\N
\N	lfe0002	\N
\N	lfe0003	\N
\N	lfe0004	\N
\N	gfwle0001	\N
\N	gfwle0002	\N
\N	gfwle0003	\N
\N	gfwle0004	\N
\N	lfra0001	\N
\N	lfra0002	\N
\N	lfra0003	\N
\N	lfra0004	\N
\N	lfra0005	\N
\N	lfra0006	\N
\N	lfra0007	\N
\N	lfra0008	\N
\N	lfra0009	\N
\N	lfra0010	\N
\N	gfwlra0001	\N
\N	gfwlra0002	\N
\N	gfwlra0003	\N
\N	gfwlra0004	\N
\N	gfwlra0005	\N
\N	gfwlra0006	\N
\N	gfwlra0007	\N
\N	gfwlra0008	\N
\N	gfwlra0009	\N
\N	gfwlra0010	\N
\N	lfrs0001	\N
\N	lfrs0002	\N
\N	lfrs0003	\N
\N	lfrs0004	\N
\N	lfrs0005	\N
\N	lfrs0006	\N
\N	lfrs0007	\N
\N	lfrs0008	\N
\N	lfrs0009	\N
\N	lfrs0010	\N
\N	gfwlrs0001	\N
\N	gfwlrs0002	\N
\N	gfwlrs0003	\N
\N	gfwlrs0004	\N
\N	gfwlrs0005	\N
\N	gfwlrs0006	\N
\N	gfwlrs0007	\N
\N	gfwlrs0008	\N
\N	gfwlrs0009	\N
\N	gfwlrs0010	\N
\.

UPDATE "Locks" SET "boundedBy" = ST_GeometryFromText('POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))', 32615) WHERE TRUE;
UPDATE "Locks" SET "pointProperty" = ST_GeometryFromText('POINT(500050 500050)', 32615) WHERE TRUE;

--
-- Data for TOC Entry ID 71 (OID 312570)
--
-- Name: OtherFeature Type: TABLE DATA Owner: public
--


COPY "Other" FROM stdin;
A Single Feature used to test returning of properties	singleFeature	SRID=32615;POLYGON((500000 500000,500000 500100,500100 500100,500100 500000,500000 500000))	SRID=32615;POINT(500050 500050)	always	sometimes	7	2002-12-02
\.

--
-- Fixes to have primary keys and no oids
--
alter table "Deletes" add column pkey serial;
alter table "Deletes" add primary key (pkey);

alter table "Fifteen" add column pkey serial;
alter table "Fifteen" add primary key (pkey);

alter table "Inserts" add column pkey serial;
alter table "Inserts" add primary key (pkey);

alter table "Lines" add column pkey serial;
alter table "Lines" add primary key (pkey);

alter table "Locks" add column pkey serial;
alter table "Locks" add primary key (pkey);

alter table "MLines" add column pkey serial;
alter table "MLines" add primary key (pkey);

alter table "MPoints" add column pkey serial;
alter table "MPoints" add primary key (pkey);

alter table "MPolygons" add column pkey serial;
alter table "MPolygons" add primary key (pkey);

alter table "Nulls" add column pkey serial;
alter table "Nulls" add primary key (pkey);

alter table "Other" add column pkey serial;
alter table "Other" add primary key (pkey);

alter table "Points" add column pkey serial;
alter table "Points" add primary key (pkey);

alter table "Polygons" add column pkey serial;
alter table "Polygons" add primary key (pkey);

alter table "Seven" add column pkey serial;
alter table "Seven" add primary key (pkey);

alter table "Updates" add column pkey serial;
alter table "Updates" add primary key (pkey);


