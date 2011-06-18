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
    * GTopo30 - (format=gtopo30)
    * ArcGrid - (format=ArcGrid)
    * GZipped ArcGrid - (format=ArcGrid-GZIP)

Beware, in the case of ArcGrid, the GetCoverage request must make sure the x and y resolution are equal, otherwise an exception will be thrown (ArcGrid is designed to have square cells).