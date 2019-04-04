.. _data_raster:

Raster data
===========

This section discusses the raster (coverage) data sources that GeoServer can access.

The standard GeoServer installation supports the loading and serving of the following data formats:

.. toctree::
   :maxdepth: 1

   geotiff
   gtopo30
   worldimage
   imagemosaic/index
   geopkg

Other data sources are supplied as GeoServer extensions.  
Extensions are downloadable modules that add functionality to GeoServer.  
Extensions are available at the `GeoServer download page <http://geoserver.org/download>`_.

.. warning:: The extension version must match the version of the GeoServer instance.

.. toctree::
   :maxdepth: 1

   arcgrid
   gdal
   oraclegeoraster
   postgisraster
   imagepyramid
   imagemosaicjdbc
   customjdbcaccess

GeoServer provides extensive facilities for controlling how rasters are accessed.
These are covered in the following sections.

.. toctree::
   :maxdepth: 1

   coverageview

   
