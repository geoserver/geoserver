-- extensions
create extension if not exists postgis;

-- cleanup
drop table if exists granule;
drop table if exists collection_ogclink;
drop table if exists product_ogclink;
drop table if exists product_metadata;
drop table if exists product_thumb;
drop table if exists product;
drop table if exists collection_metadata;
drop table if exists collection_layer;
drop table if exists collection;

-- the collections and the attributes describing them
create table collection (
  "id" serial primary key,
  "name" varchar,
  "primary" boolean,
  "htmlDescription" text,
  "footprint" geometry(Polygon, 4326),
  "timeStart" timestamp,
  "timeEnd" timestamp,
  "productCqlFilter" varchar,
  "masked" boolean,
  "eoIdentifier" varchar unique,
  "eoProductType" varchar,
  "eoPlatform" varchar,
  "eoPlatformSerialIdentifier" varchar,
  "eoInstrument" varchar,
  "eoSensorType" varchar, -- this is configurable, so no checks on values anymore
  "eoCompositeType" varchar,
  "eoProcessingLevel" varchar,
  "eoOrbitType" varchar,
  "eoSpectralRange" varchar,
  "eoWavelength" int,
  "eoSecurityConstraints" boolean,
  "eoDissemination" varchar,
  "eoAcquisitionStation" varchar
);
-- index all (really, this is a search engine)
-- manually generated indexes
create index "idx_collection_footprint" on collection using GIST("footprint");
-- the following indexes have been generated calling
-- SELECT 'CREATE INDEX "idx_' || table_name || '_' || column_name || '" ON ' || table_name || ' ("' || column_name || '");'   FROM information_schema.columns WHERE table_schema = current_schema() and table_name = 'collection' and (column_name like 'eo%' or column_name like 'opt%' or column_name like 'sar%' or column_name like 'time%');
CREATE INDEX "idx_collection_timeStart" ON collection ("timeStart");
CREATE INDEX "idx_collection_timeEnd" ON collection ("timeEnd");
CREATE INDEX "idx_collection_eoIdentifier" ON collection ("eoIdentifier");
CREATE INDEX "idx_collection_eoProductType" ON collection ("eoProductType");
CREATE INDEX "idx_collection_eoPlatform" ON collection ("eoPlatform");
CREATE INDEX "idx_collection_eoPlatformSerialIdentifier" ON collection ("eoPlatformSerialIdentifier");
CREATE INDEX "idx_collection_eoInstrument" ON collection ("eoInstrument");
CREATE INDEX "idx_collection_eoSensorType" ON collection ("eoSensorType");
CREATE INDEX "idx_collection_eoCompositeType" ON collection ("eoCompositeType");
CREATE INDEX "idx_collection_eoProcessingLevel" ON collection ("eoProcessingLevel");
CREATE INDEX "idx_collection_eoOrbitType" ON collection ("eoOrbitType");
CREATE INDEX "idx_collection_eoSpectralRange" ON collection ("eoSpectralRange");
CREATE INDEX "idx_collection_eoWavelength" ON collection ("eoWavelength");
CREATE INDEX "idx_collection_eoSecurityConstraints" ON collection ("eoSecurityConstraints");
CREATE INDEX "idx_collection_eoDissemination" ON collection ("eoDissemination");
CREATE INDEX "idx_collection_eoAcquisitionStation" ON collection ("eoAcquisitionStation");

-- the layer publishing information, if any
create table collection_layer (
  "lid" serial primary key,
  "cid" int references collection("id") on delete cascade,
  "workspace" varchar,
  "layer" varchar,
  "separateBands" boolean,
  "bands" varchar,
  "browseBands" varchar,
  "heterogeneousCRS" boolean,
  "mosaicCRS" varchar,
  "defaultLayer" boolean
);

-- the iso metadata storage (large text, not used for search, thus separate table)
create table collection_metadata (
  "mid" int primary key references collection("id"),
  "metadata" text
);

-- the products and attributes describing them
create table product (
  "id" serial primary key,
  "htmlDescription" text,
  "footprint" geometry(Polygon, 4326),
  "timeStart" timestamp,
  "timeEnd" timestamp,
  "originalPackageLocation" varchar,
  "originalPackageType" varchar,
  "thumbnailURL" varchar,
  "quicklookURL" varchar,
  "crs" varchar,
  "eoIdentifier" varchar unique,
  "eoParentIdentifier" varchar references collection("eoIdentifier") on delete cascade,
  "eoProductionStatus" varchar,
  "eoAcquisitionType" varchar check ("eoAcquisitionType" in ('NOMINAL', 'CALIBRATION', 'OTHER')),
  "eoOrbitNumber" int,
  "eoOrbitDirection" varchar check ("eoOrbitDirection" in ('ASCENDING', 'DESCENDING')),
  "eoTrack" int,
  "eoFrame" int,
  "eoSwathIdentifier" text,
  "optCloudCover" int check ("optCloudCover" between 0 and 100),
  "optSnowCover" int check ("optSnowCover" between 0 and 100),
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
  "eoAcquisitionStation" varchar,
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
  "atmVerticalRange" float[],
  "atmVerticalResolution" float[],
  "atmSpecies" varchar[],
  "atmSpeciesError" float[],
  "atmUnit" varchar[],
  "atmAlgorithmName" varchar[],
  "atmAlgorithmVersion" varchar[]
);

-- index all (really, this is a search engine)
-- manually generated indexes
create index "idx_product_footprint" on product using GIST("footprint");
-- the following indexes have been generated adding
-- SELECT 'CREATE INDEX "idx_' || table_name || '_' || column_name || '" ON ' || table_name || ' ("' || column_name || '");'   FROM information_schema.columns WHERE table_name = 'product' and column_name like 'eo%' or column_name like 'opt%' or column_name like 'sar%' or column_name like 'time%';
 CREATE INDEX "idx_product_timeStart" ON product ("timeStart");
 CREATE INDEX "idx_product_timeEnd" ON product ("timeEnd");
 CREATE INDEX "idx_product_eoParentIdentifier" ON product ("eoParentIdentifier");
 CREATE INDEX "idx_product_eoProductionStatus" ON product ("eoProductionStatus");
 CREATE INDEX "idx_product_eoAcquisitionType" ON product ("eoAcquisitionType");
 CREATE INDEX "idx_product_eoOrbitNumber" ON product ("eoOrbitNumber");
 CREATE INDEX "idx_product_eoOrbitDirection" ON product ("eoOrbitDirection");
 CREATE INDEX "idx_product_eoTrack" ON product ("eoTrack");
 CREATE INDEX "idx_product_eoFrame" ON product ("eoFrame");
 CREATE INDEX "idx_product_eoSwathIdentifier" ON product ("eoSwathIdentifier");
 CREATE INDEX "idx_product_optCloudCover" ON product ("optCloudCover");
 CREATE INDEX "idx_product_optSnowCover" ON product ("optSnowCover");
 CREATE INDEX "idx_product_eoProductQualityStatus" ON product ("eoProductQualityStatus");
 CREATE INDEX "idx_product_eoProductQualityDegradationStatus" ON product ("eoProductQualityDegradationStatus");
 CREATE INDEX "idx_product_eoProcessorName" ON product ("eoProcessorName");
 CREATE INDEX "idx_product_eoProcessingCenter" ON product ("eoProcessingCenter");
 CREATE INDEX "idx_product_eoCreationDate" ON product ("eoCreationDate");
 CREATE INDEX "idx_product_eoModificationDate" ON product ("eoModificationDate");
 CREATE INDEX "idx_product_eoProcessingDate" ON product ("eoProcessingDate");
 CREATE INDEX "idx_product_eoSensorMode" ON product ("eoSensorMode");
 CREATE INDEX "idx_product_eoArchivingCenter" ON product ("eoArchivingCenter");
 CREATE INDEX "idx_product_eoProcessingMode" ON product ("eoProcessingMode");
 CREATE INDEX "idx_product_eoAvailabilityTime" ON product ("eoAvailabilityTime");
 CREATE INDEX "idx_product_eoAcquisitionStation" ON product ("eoAcquisitionStation");
 CREATE INDEX "idx_product_eoAcquisitionSubtype" ON product ("eoAcquisitionSubtype");
 CREATE INDEX "idx_product_eoStartTimeFromAscendingNode" ON product ("eoStartTimeFromAscendingNode");
 CREATE INDEX "idx_product_eoCompletionTimeFromAscendingNode" ON product ("eoCompletionTimeFromAscendingNode");
 CREATE INDEX "idx_product_eoIlluminationAzimuthAngle" ON product ("eoIlluminationAzimuthAngle");
 CREATE INDEX "idx_product_eoIlluminationZenithAngle" ON product ("eoIlluminationZenithAngle");
 CREATE INDEX "idx_product_eoIlluminationElevationAngle" ON product ("eoIlluminationElevationAngle");
 CREATE INDEX "idx_product_sarPolarisationMode" ON product ("sarPolarisationMode");
 CREATE INDEX "idx_product_sarPolarisationChannels" ON product ("sarPolarisationChannels");
 CREATE INDEX "idx_product_sarAntennaLookDirection" ON product ("sarAntennaLookDirection");
 CREATE INDEX "idx_product_sarMinimumIncidenceAngle" ON product ("sarMinimumIncidenceAngle");
 CREATE INDEX "idx_product_sarMaximumIncidenceAngle" ON product ("sarMaximumIncidenceAngle");
 CREATE INDEX "idx_product_sarDopplerFrequency" ON product ("sarDopplerFrequency");
 CREATE INDEX "idx_product_sarIncidenceAngleVariation" ON product ("sarIncidenceAngleVariation");
 CREATE INDEX "idx_product_eoResolution" ON product ("eoResolution");
 CREATE INDEX "idx_product_atmVerticalRange" on product using GIN("atmVerticalRange");
 CREATE INDEX "idx_product_atmVerticalResolution" on product using GIN("atmVerticalResolution");
 CREATE INDEX "idx_product_atmSpecies" on product using GIN("atmSpecies");
 CREATE INDEX "idx_product_atmSpeciesError" on product using GIN("atmSpeciesError");
 CREATE INDEX "idx_product_atmAlgorithmName" on product using GIN("atmAlgorithmName");
 CREATE INDEX "idx_product_atmAlgorithmVersion" on product using GIN("atmAlgorithmVersion");
 

 -- the eo metadata storage (large files, not used for search, thus separate table)
create table product_metadata (
  "mid" int primary key references product("id") on delete cascade,
  "metadata" text
);

-- the eo thumbs storage (small binary files, not used for search, thus separate table)
create table product_thumb (
	"tid" int primary key references product("id") on delete cascade,
	"thumb" bytea
);

-- links for collections
create table collection_ogclink (
  "lid" serial primary key,
  "collection_id" int references collection("id") on delete cascade,
  "offering" varchar,
  "method" varchar,
  "code" varchar,
  "type" varchar,
  "href" varchar
);

-- links for products
create table product_ogclink (
  "lid" serial primary key,
  "product_id" int references product("id") on delete cascade,
  "offering" varchar,
  "method" varchar,
  "code" varchar,
  "type" varchar,
  "href" varchar
); 

-- the granules table (might be abstract, and we can use partitioning)
create table granule (
  "gid" serial primary key,
  "product_id" int not null references product("id") on delete cascade,
  "band" varchar,
  "location" varchar not null,
  "the_geom" geometry(Polygon, 4326) not null
);

-- manually generated indexes
CREATE INDEX "idx_granule_the_geom" ON granule USING GIST("the_geom");
