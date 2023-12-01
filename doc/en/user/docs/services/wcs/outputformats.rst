.. _wcs_output_formats:

WCS output formats
==================

WCS output formats are configured coverage by coverage. The current list of output formats follows:

Images:

    * JPEG - (format=jpeg)
    * GIF - (format=gif)
    * PNG - (format=png)
    * Tiff - (format=tif)
    * BMP - (format=bmp)

Georeferenced formats:

    * GeoTiff - (format=geotiff)
    * GML Coverage - (format=application/gml+xml)

The GML Coverage format is described by the `OGC Coverage Implementation Schema <https://portal.ogc.org/files/?artifact_id=48553>`_, its components are also used to describe coverage metadata in WCS 2.0 ``DescribeCoverage`` responses. 