-- extensions
create extension if not exists postgis;
create extension if not exists hstore;

-- cleanup
drop table if exists collection_ogclink;
drop table if exists product_ogclink;
drop table if exists ogclink;
drop table if exists product;
drop table if exists collection;

-- the collections and the attributes describing them
create table collection (
  id serial primary key,
  name varchar,
  "primary" boolean,
  "htmlDescription" text,
  isoMetadata text,
  footprint geometry(Polygon, 4326),
  "timeStart" timestamp,
  "timeEnd" timestamp,
  productCqlFilter varchar,
  masked boolean,
  "eoIdentifier" varchar unique,
  "eoProductType" varchar,
  "eoPlatform" varchar,
  "eoPlatformSerialIdentifier" varchar,
  "eoInstrument" varchar,
  "eoSensorType" varchar check ("eoSensorType" in ('OPTICAL', 'RADAR', 'ALTIMETRIC', 'ATMOSPHERIC', 'LIMB')),
  "eoCompositeType" varchar,
  "eoProcessingLevel" varchar,
  "eoOrbitType" varchar,
  "eoSpectralRange" varchar,
  "eoWavelenght" int,
  "eoSecurityConstraints" boolean,
  "eoDissemination" varchar,
  "eoAcquisitionStation" varchar,
  "customAttributes" hstore
);

-- the products and attributes describing them
create table product (
  id serial primary key,
  name varchar,
  "htmlDescription" text,
  "omEoMetadata" text,
  location geometry(Polygon, 4326),
  "timeStart" timestamp,
  "timeEnd" timestamp,
  "originalPackageLocation" varchar,
  "thumbnailURL" varchar,
  "quicklookURL" varchar,
  "eoParentIdentifier" varchar references collection("eoIdentifier"),
  "eoProductionStatus" varchar,
  "eoAcquisitionType" varchar check ("eoAcquisitionType" in ('NOMINAL', 'CALIBRATION', 'OTHER')),
  "eoOrbitNumber" int,
  "eoOrbitDirection" varchar check ("eoOrbitDirection" in ('ASCENDING', 'DESCENDING')),
  "eoTrack" int,
  "eoFrame" int,
  "eoSwathIdentifier" text,
  "optCloudCover" int check ("optCloudCover" between 0 and 100),
  "optSnowCover" int check ("optCloudCover" between 0 and 100),
  "eoProductQualityStatus" varchar check ("eoProductQualityStatus" in ('NOMINAL', 'DEGRADED')),
  "eoProductQualityDegradationStatus" varchar,
  "eoProcessorName" varchar,
  "eoProcessingCenter" varchar,
  "eoCreationDate" timestamp,
  "eoModificationDate" timestamp,
  "eoProcessingDate" timestamp,
  "eoSensorMode" varchar,
  "eoArchivingCenter" varchar,
  "eoProcessingMode" varchar,
  "eoAvailabilityTime" timestamp,
  "eoAcquisitionStation" timestamp,
  "eoAcquisitionSubtype" varchar,
  "eoStartTimeFromAscendingNode" int,
  "eoCompletionTimeFromAscendingNode" int,
  "eoIlluminationAzimuthAngle" float,
  "eoIlluminationZenithAngle" float,
  "eoIlluminationElevationAngle" float,
  "sarPolarisationMode" varchar check ("sarPolarisationMode" in ('S', 'D', 'T', 'Q', 'UNDEFINED')),
  "sarPolarisationChannels" varchar check ("sarPolarisationChannels" in ('horizontal', 'vertical')),
  "sarAntennaLookDirection" varchar check ("sarAntennaLookDirection" in ('LEFT', 'RIGHT')),
  "sarMinimumIncidenceAngle" float,
  "sarMaximumIncidenceAngle" float,
  "sarDopplerFrequency" float,
  "sarIncidenceAngleVariation" float,
  "eoResolution" float,
  "customAttributeValues" hstore
);

-- ogc links (abstract)
create table ogclink (
  id serial primary key,
  offering varchar,
  method varchar,
  code varchar,
  "type" varchar,
  href varchar
);

-- links for collections
create table collection_ogclink (
  collection_id int references collection(id)
) inherits(ogclink);

-- links for products
create table product_ogclink (
  product_id int references product(id)
) inherits(ogclink);
