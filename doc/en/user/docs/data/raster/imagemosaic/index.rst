.. _data_imagemosaic:

ImageMosaic
=====================

The ImageMosaic data store allows the creation of a mosaic from a number of georeferenced rasters.

The mosaic operation creates a mosaic from two or more source images. This operation could be used to assemble a set of overlapping geospatially rectified images into a contiguous image. It could also be used to create a montage of photographs such as a panorama.

The plugin can be used with GeoTIFFs, as well as rasters accompanied by a world file (``.wld``, ``.pgw`` for PNG files, ``.jgw`` for JPG files, etc.). In addition, if imageIO-ext :ref:`GDAL extension <data_gdal>` is properly installed, the plugin can also serve all the formats supported by it such as MrSID, ECW, JPEG2000. It also supports NetCDF and GRIB.

.. toctree::
   :maxdepth: 2

   configuration
   tutorial
