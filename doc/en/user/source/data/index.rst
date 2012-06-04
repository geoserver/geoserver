.. _data:

Working with Data
=================

This section discusses the data sources that can GeoServer can read and access.

GeoServer allows the loading and serving of the following data formats by default:

* Vector data formats
   * Shapefiles (including directories of shapefiles)
   * PostGIS databases (with or without JNDI)
   * External WFS layers
   * Java Properties files
* Raster data formats
   * ArcGrid
   * GeoTIFF
   * Gtopo30
   * ImageMosaic
   * WorldImage
* Other data formats
   * External WMS layers

Other data sources require the use of GeoServer extensions, extra downloads that add functionality to GeoServer.  These extensions are always available on the `GeoServer download page <http://geoserver.org/display/GEOS/Download>`_.

.. warning:: If an extension is required to load the data source, make sure to match the version of the extension to the version of the GeoServer instance!

.. toctree::
   :maxdepth: 1

   shapefile
   directory
   properties
   arcgrid
   geotiff
   gtopo30
   imagemosaic
   worldimage
   gml
   featurepregen
   vpf
   gdal
   imagepyramid
   imagemosaicjdbc
   oraclegeoraster
   customjdbcaccess
   app-schema/index

   
