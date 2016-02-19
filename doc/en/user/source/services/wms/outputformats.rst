.. _wms_output_formats:

WMS output formats
==================

WMS returns images in a number of possible formats.  This page shows a list of the output formats.  The syntax for setting an output format is::

   format=<format>

where ``<format>`` is any of the options below.

.. note:: The list of output formats supported by a GeoServer instance can be found by a WMS :ref:`wms_getcap` request.

.. list-table::
   :widths: 30 30 40
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - PNG
     - ``format=image/png``
     - Default
   * - PNG8
     - ``format=image/png8``
     - Same as PNG, but computes an optimal 256 color (8 bit) palette, so the image size is usually smaller
   * - JPEG
     - ``format=image/jpeg``
     -
   * - GIF
     - ``format=image/gif``
     -
   * - TIFF
     - ``format=image/tiff``
     -
   * - TIFF8
     - ``format=image/tiff8``
     - Same as TIFF, but computes an optimal 256 color (8 bit) palette, so the image size is usually smaller
   * - GeoTIFF
     - ``format=image/geotiff``
     - Same as TIFF, but includes extra GeoTIFF metadata
   * - GeoTIFF8
     - ``format=image/geotiff8``
     - Same as TIFF, but includes extra GeoTIFF metadata and computes an optimal 256 color (8 bit) palette, so the image size is usually smaller
   * - SVG
     - ``format=image/svg``
     -
   * - PDF
     - ``format=application/pdf``
     -
   * - GeoRSS
     - ``format=rss``
     -
   * - KML
     - ``format=kml``
     -
   * - KMZ
     - ``format=kmz``
     -
   * - OpenLayers
     - ``format=application/openlayers``
     - Generates an OpenLayers HTML application.

   * - UTFGrid
     - ``format=application/json;type=utfgrid``
     - Generates an `UTFGrid 1.3 <https://github.com/mapbox/utfgrid-spec/blob/master/1.3/utfgrid.md>`_ JSON response. Requires vector output, either from a vector layer, or
       from a raster layer turned into vectors by a rendering transformation.
     