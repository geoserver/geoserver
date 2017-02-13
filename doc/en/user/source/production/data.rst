.. _production_data:

Data Considerations
===================

Use an external data directory
******************************

GeoServer comes with a built-in data directory.  However, it is a good idea to separate the data from the application.  
Using an external data directory allows for much easier upgrades, since there is no risk of configuration information being overwritten.  An external data directory also makes it easy to transfer your configuration elsewhere if desired.  To point to an external data directory, you only need  to edit the :file:`web.xml` file.  If you are new to GeoServer, you can copy (or just move) the data directory that comes with GeoServer to another location.

Use a spatial database
**********************

Shapefiles are a very common format for geospatial data. But if you are running GeoServer in a production environment, it is better to use a spatial database such as `PostGIS <http://www.postgis.org>`_.  This is essential if doing transactions (WFS-T). Most spatial databases provide shapefile conversion tools. Although there are many options for spatial databases (see the section on :ref:`data_database`), PostGIS is recommended. Oracle, DB2, and ArcSDE are also supported.

Pick the best performing coverage formats
*****************************************

There are very significant differences between performance of the various coverage formats.

Serving big coverage data sets with good performance requires some knowledge and tuning, since usually data is set up for distribution and archival. The following tips try to provide you with a base knowledge of how data restructuring affects performance, and how to use the available tools to get optimal data serving performance.

Choose the right format
-----------------------

The first key element is choosing the right format. Some formats are designed for data exchange, others for data rendering and serving. A good data serving format is binary, allows for multi-resolution extraction, and provides support for quick subset extraction at native resolutions.

Examples of such formats are GeoTiff, ECW, JPEG 2000 and MrSid. ArcGrid on the other hand is an example of format that's particularly ill-suited for large dataset serving (it's text based, no multi-resolution, and we have to read it fully even to extract a data subset in the general case).

GeoServer supports MrSID, ECW and JPEG 2000 through the GDAL Image Format plugin.  MrSID is the easiest to work with, as their reader is now available under a GeoServer compatible open source format.  If you have ECW files you have several non-ideal options.  If you are only using GeoServer for educational or non-profit purposes you can use the plugin for free.  If not you need to buy a license, since it's server software.  You could also use GDAL to convert it to MrSID or tiled GeoTiffs.  If your files are JPEG 2000 you can use the utilities of ECW and MrSID software.  But the fastest is Kakadu, which requires a license.  

Setup Geotiff data for fast rendering
-------------------------------------

As soon as your Geotiffs gets beyond some tens of megabytes you'll want to add the following capabilities:

    * inner tiling
    * overviews

Inner tiling sets up the image layout so that it's organized in tiles instead of simple stripes (rows). This allows much quicker access to a certain area of the geotiff, and the GeoServer readers will leverage this by accessing only the tiles needed to render the current display area. The following sample command instructs `gdal_translate <http://www.gdal.org/gdal_translate.html>`_ to create a tiled
`geotiff <http://www.gdal.org/frmt_gtiff.html>`_.

.. code-block:: xml

   gdal_translate -of GTiff -projwin -180 90 -50 -10 -co "TILED=YES" bigDataSet.ecw myTiff.tiff

An overview is a downsampled version of the same image, that is, a zoomed out version, which is usually much smaller. When GeoServer needs to render the Geotiff, it'll look for the most appropriate overview as a starting point, thus reading and converting way less data. Overviews can be added using 
`gdaladdo <http://www.gdal.org/gdaladdo.html>`_, or the the OverviewsEmbedded command included in Geotools. Here is a sample of using gdaladdo to add overviews that are downsampled 2, 4, 8 and 16 times compared to the original:

.. code-block:: xml

   gdaladdo -r average mytiff.tif 2 4 8 16

As a final note, Geotiff supports various kinds of compression, but we do suggest to not use it. Whilst it allows for much smaller files, the decompression process is expensive and will be performed on each data access, significantly slowing down rendering. In our experience, the decompression time is higher than the pure disk data reading.

Handling huge data sets
-----------------------

If you have really huge data sets (several gigabytes), odds are that simply adding overviews and tiles does not cut it, making intermediate resolution serving slow. This is because tiling occurs only on the native resolution levels, and intermediate overviews are too big for quick extraction.

So, what you need is a way to have tiling on intermediate levels as well. This is supported by the ImagePyramid plugin.

This plugin assumes you have create various seamless image mosaics, each for a different resolution level of the original image. In the mosaic, tiles are actual files (for more info about mosaics, see the :ref:`data_imagemosaic`). The whole pyramid structures looks like the following:


.. code-block:: xml


   rootDirectory
       +- pyramid.properties
       +- 0
          +- mosaic metadata files
          +- mosaic_file_0.tiff
          +- ...
          +- mosiac_file_n.tiff
       +- ...
       +- 32
          +- mosaic metadata files
          +- mosaic_file_0.tiff
          +- ...
          +- mosiac_file_n.tiff

Creating a pyramid by hand can theoretically be done with gdal, but in practice it's a daunting task that would require some scripting, since gdal provides no "tiler" command to extract regular tiles out of an image, nor one to create a downsampled set of tiles. As an alternative, you can use the geotools PyramidBuilder tool (documentation on how to use this is pending, contact the developers if you need to use it).
