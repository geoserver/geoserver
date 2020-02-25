.. _opensearch_database:

The JDBC store database structure
=================================

The JDBC store uses a conventional relational structure, depicted in the following picture:

.. figure:: images/dbschema.png

So a ``collection`` has its own primary search attributes, as well as:

* A ISO metadata document as large associated text
* Zero or more OGC links pointing to where the collection is published
* Layer publishing information (for auto-generation of mosaic, layer and eventual coverage view in case the actual data resides locally)
* One or more products

A ``product`` in turn is associated to:

* A O&M metadata document as large associated text
* A thumbnail image, in PNG or JPEG format
* Zero or more OGC links pointing to where the product is published

The ``granule`` table is designed to contain per product file information in case there
is a desire to publish the actual data from the same local GeoServer (but in general, OGC services
might be missing or provided by a separate server).

Collections
-----------

The collection table currently looks as follows (check the SQL file in the installation instructions for
a more up to date version of it):

.. code-block:: sql

    create table collection (
      "id" serial primary key,
      "name" varchar,
      "primary" boolean,
      "htmlDescription" text,
      "footprint" geography(Polygon, 4326),
      "timeStart" timestamp,
      "timeEnd" timestamp,
      "productCqlFilter" varchar,
      "masked" boolean,
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
      "eoWavelength" int,
      "eoSecurityConstraints" boolean,
      "eoDissemination" varchar,
      "eoAcquisitionStation" varchar
    );

Most of the attributes should be rather self-explanatory to those familiar with OGC Earth Observation terminology.
Each attribute prefixed by "eo" is exposed as a search attribute in OpenSearch, the structure can be modified
by adding extra attributes and they will show up and made searchable.

Specific attributes notes:

* A ``primary`` collection is normally linked to a particular satellite/sensor and contains its own products.
  Setting "primary" to false makes the collection "virtual" and the ``productCQLFilter`` field should be filled with
  a CQL filter that will collect all the products in the collection (warning, virtual collections are largerly
  untested at the moment)
* The ``footprint`` field is used for spatial searches, while ``timeStart`` and ``timeEnd`` are used for
  temporal ones
* The ``htmlDescription`` drives the generation of the visible part of the Atom OpenSearch response, see the
  dedicated section later to learn more about filling it

The ``collection_metadata`` table contains the ISO metadata for the given collection.
The OpenSearch module has no understanding of its contents, it will simply return it as is, allowing for
extra flexibility but also moving the responsibility for correctness checks completely to the author.

The ``collection_ogclink`` table contains the OGC links towards the services providing visualization and
download access to the collection contents. See the "OGC links" section to gather more information about it.

Products
--------

The product table currently looks as follows (check the SQL file in the installation instructions for
a more up to date version of it):

.. code-block:: sql

  -- the products and attributes describing them
  create table product (
    "id" serial primary key,
    "htmlDescription" text,
    "footprint" geography(Polygon, 4326),
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
    "eoResolution" float
  );

Notes on the attributes:

* The ``footprint`` field is used for spatial searches, while ``timeStart`` and ``timeEnd`` are used for
  temporal ones
* The ``htmlDescription`` drives the generation of the visible part of the Atom OpenSearch response, see the
  dedicated section later to learn more about filling it
* The ``crs`` attribute is optional and is used only for automatic layer publishing for collections having
  heterogeneous CRS products. It must contain a "EPSG:XYWZ" expression (but the product footprint still
  need to be expressed in WGS84, east/north oriented).
* The EO search attributes need to be filled according to the nature of the product, ``eo`` prefixes generic
  EOP attributes, ``opt`` optical ones, ``sar`` radar ones, ``atm`` altimetric, ``lmb`` limbic, ``ssp``
  Synthesis and Systematic Product. New attributes can be added based on the above prefixes (at the time
  of writing only optical and sar attributes have been tested)

The ``product_metadata`` table contains the O&M metadata for the given product.
The OpenSearch module has no understanding of its contents, it will simply return it as is, allowing for
extra flexibility but also moving the responsibility for correctness checks completely to the author.

The ``product_thumb`` table contains the product thumbnail, in PNG or JPEG format, for display
in the OpenSearch Atom output.

The ``product_ogclink`` table contains the OGC links towards the services providing visualization and
download access to the collection contents. See the "OGC links" section to gather more information about it.

The ``htmlDescription`` field
------------------------------

The ``htmlDescription`` is used to fill the user visible part of a OpenSearch ATOM response.
The contents are completely freeform, but some variable can be put in the HTML that GeoServer will replace:

* ``${QUICKLOOK_URL}`` points to the product quicklook (at the time of writing, same as the thumbnail)
* ``${THUMB_URL}`` points to the product thumbnail
* ``${ATOM_URL}`` points to the specific Atom record at hand (either the collection or product one)
* ``${OM_METADATA_URL}`` points to the product O&M metadata
* ``${ISO_METADATA_LINK}`` points to the ISO metadata link

OGC links
---------

The OpenSearch module implements "OGC cross linking" by adding pointers to OGC services
for to collection/product visualization and download.

.. code-block:: sql

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

This is done by adding a set of ``owc:offering`` elements in the Atom response, mapping directly
from the table contents:

.. code-block:: xml

    <owc:offering code="http://www.opengis.net/spec/owc/1.0/req/atom/wcs">
      <owc:operation method="GET" code="GetCapabilities" href="http://localhost/sentinel2/sentinel2-TCI/ows?service=WCS&amp;version=2.0.1&amp;request=GetCapabilities" type="application/xml"/>
    </owc:offering>
    <owc:offering code="http://www.opengis.net/spec/owc/1.0/req/atom/wmts">
      <owc:operation method="GET" code="GetCapabilities" href="http://localhost/sentinel2/sentinel2-TCI/gwc/service/wmts?REQUEST=GetCapabilities" type="application/xml"/>
    </owc:offering>
    <owc:offering code="http://www.opengis.net/spec/owc/1.0/req/atom/wms">
      <owc:operation method="GET" code="GetCapabilities" href="http://localhost/sentinel2/sentinel2-TCI/ows?service=wms&amp;version=1.3.0&amp;request=GetCapabilities" type="application/xml"/>
      <owc:operation method="GET" code="GetMap" href="http://localhost/sentinel2/sentinel2:sentinel2-TCI/wms?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetMap&amp;FORMAT=image%2Fjpeg&amp;STYLES&amp;LAYERS=sentinel2%3Asentinel2-TCI&amp;SRS=EPSG%3A4326&amp;WIDTH=800&amp;HEIGHT=600&amp;BBOX=-180%2C-90%2C180%2C90" type="image/jpeg"/>
    </owc:offering>

The contents of the tables need to be filled with the sane named elements of a OWC offering,
the ``href`` one can contain a ``${BASE_URL}`` variable that GeoServer will replace with its
own base URL.

The granule table
-----------------

The granule table can be filled with information about the actual raster files making up
a certain product in order to publish the collection as a GeoServer image mosaic:

.. code-block:: sql

  -- the granules table (might be abstract, and we can use partitioning)
  create table granule (
    "gid" serial primary key,
    "product_id" int not null references product("id") on delete cascade,
    "band" varchar,
    "location" varchar not null,
    "the_geom" geometry(Polygon, 4326) not null
  );

The granules associated to a product can have different topologies:

* A single raster file containing all the information about the product
* Multiple raster files splitting the products spatially in regular tiles
* Multiple raster files splitting the product wavelenght wise
* A mix of the two above

Notes about the columns:

* The ``band`` column need to be filled only for products split in several files by bands, at the time of
  writing it needs to be a progressive integer starting from 1 (the module will hopefully allow more meaningful band names in the future)
* The ``location`` is the absolute path of the file
* The ``the_geom`` field is a polygon in WGS84, regardless of what the actual footprint of the file is. The polygon must represent the rectangular extend of the raster file,
  not its valid area (masking is to be treated separately, either with sidecar mask files or with NODATA pixels)
