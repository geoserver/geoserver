.. _community_vector_mosaic_delegate:

Vector Mosaic Datastore Delegate Requirements
=============================================

The Vector Mosaic Datastore Delegate is a datastore that contains references to the vector granule datastores, bounding polygon or multipolygon geometry to delineate the index area, and optionally other attributes that can be queried in order to return vector granules.

The delegate datastore can be in any format that GeoServer supports but there are two required fields:

* There must be a geometry field representing the index spatial area in either Polygon or MultiPolygon form. There are not requirements on the name of such field.
* There should be a field called ``params``, in text format, that contains either URIs pointing at granule resources like shapefiles -or- a configuration string in .properties format. (See `Java Properties file <https://en.wikipedia.org/wiki/.properties>`_ for more details about the format).

Any other field beyond the two required can serve as queryable/filterable attribute, and will be used to narrow the number of potential granule vectors that are searched by a query.  The non-required parameters will be combined with the vector granule parameters to create the output feature type.

An example of a delegate in property datastore format can be found `here <https://github.com/geotools/geotools/blob/main/modules/unsupported/vector-mosaic/src/test/resources/org.geotools.vectormosaic.data/mosaic_delegate.properties>`_. The ``name`` and ``type`` fields can be used to filter the granules, while ``params` contains the location of the granule file and ``geom`` its footprint. 


Creating an Index with ogrtindex
================================

The `ogrtindex <https://gdal.org/programs/ogrtindex.html>`_ commandline tool from the GDAL library can be used to collect all data sets in a directory, and create an index table for it. The format of the location is slightly different than the one GeoServer expects, as it uses a ``location,tableIndex`` format, so a quick SQL needs to be run to make it match.  

Here is an example that generates a delegate shapefile from a directory of shapefiles.  The third step below uses ``ogr2ogr`` commandline to trim a comma and number that ``ogrtindex`` appends to the end of the granule reference, and to turn the file location into a valid URL.

#. Switch to directory with the shapefiles
#. ogrtindex  -write_absolute_path -tileindex "params" delegate_raw.shp \*.shp
#. ogr2ogr delegate.shp delegate_raw.shp -dialect SQLite -sql "SELECT Geometry,'file://'||SUBSTR(params,1,LENGTH(params)-2) AS params from delegate_raw"

The ``delegate.shp`` shapefile can then be published as a store in GeoServer (no need to publish the layer), and then the mosaic store can be created, referencing to it:

For example, let's say one downloads the `TIGER shapefile <https://www.census.gov/geographies/mapping-files/time-series/geo/tiger-line-file.html>`_ for the ``PLACE`` theme, 
providing a shapefile with urban areas for each of the US states:

.. figure:: images/places-files.png
   :align: center

Scripts exist that help with the bulk download of the files for a given theme and year, e.g.
`get-tiger <https://github.com/fitnr/get-tiger>`_.

``ogrtindex`` and ``ogr2ogr`` can be used to generate a index shapefile, which will be
then configured in GeoServer, and then serve as the base for mosaic store:

.. figure:: images/places-stores.png
   :align: center

   *The store containing the delegate/index table, and the mosaic store*

.. figure:: images/places-mosaic-config.png
   :align: center

   *The mosaic store refers to the delegate store by name*

The ``connectionParameterKey`` is ``url``, as that's what the Shapefile datastore is looking for,
a parameter named ``url`` with the location of the shapefile to open. The preferred SPI is
setup to the Shapefile store to speed up the lookup of the granule store (it can be omitted,
with a small performance drop).

The mosaic layer can then be published in GeoServer, rendering all the required shapefiles
in a single map:

.. figure:: images/places-mosaic.png
   :align: center

Creating FlatGeobuf Granules with ogr2ogr and ogrtindex
======================================================================

`FlatGeobuf <https://flatgeobuf.org>`_ files make for an excellent option for cloud storage of granule data due the built in support for R-Tree indices and the use of `HTTP Range requests <https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests>`_ to limit the amount of data streamed over the network.

``ogr2ogr`` can be used to convert a directory of shapefiles into a directory of indexed Flatgeobufs using a simple bash script like below

.. code-block:: bash

   #!/bin/bash
   shopt -s nullglob
   FILES="/data/tiger/*.shp"
   for f in $FILES
   do
     fgbfilename="$(basename $f .shp).fgb"
     ogr2ogr -f FlatGeobuf $fgbfilename $f -nlt PROMOTE_TO_MULTI -lco SPATIAL_INDEX=YES
   done

Here is an example that generates a delegate shapefile from the directory of FlatGeobufs.  The third step below uses ``ogr2ogr`` commandline to trim a comma and number that ``ogrtindex`` appends to the end of the granule reference, and to turn the file location into a valid URL.  Note the exclusion of the ``write_absolute_path``.  Instead we append the AWS S3 bucket URL to the generated filename.  

#. Switch to directory with the FlatGeoBufs.
#. ogrtindex -tileindex "params" delegate_raw.shp \*.fgb
#. ogr2ogr delegate.shp delegate_raw.shp -dialect SQLite -sql "SELECT Geometry,'https://mybucketname.s3.amazonaws.com/'||SUBSTR(params,1,LENGTH(params)-2) AS params from delegate_raw"
#. Upload FlatGeobuf granule files to the S3 bucket referenced in the earlier step (and confirm that the bucket contents are publicly available).

At this point you can publish the ``delegate.shp`` shapefile as a store in GeoServer as described in the previous example or you can load it into PostGIS before publication (see `Smart Data Loader <../smart-data-loader/data-store.html>`_ for a tool for creating the PostGIS store).  A PostGIS delegate is especially useful mosaics that might change over time due to support for concurrent edits, high rate loading and transactions.  Note that because the granule references in the index are HTTPS URLs the index FlatgeoBuf can be hosted anywhere that your GeoServer installation can access.
