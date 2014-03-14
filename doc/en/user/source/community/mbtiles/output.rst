GeoPackage WMS Output Format
============================

Any WMS :ref:`wms_getmap` request can be returned in the form of a Geopackage by specifying ``format=mbtiles`` as output format (see :ref:`wms_output_formats`). \
The returned result will be an MBTiles file with a single tile layer. 

The following additional parameters can be passed on using :ref:`format_options`:
  * ``tileset_name``: name to be used for tileset in mbtiles file (default is name of layer(s)).
  * ``min_zoom``, ``max_zoom``, ``min_column``, ``max_column``, ``min_row``, ``max_row``: set the minimum and maximum zoom level, column, and rows
  * ``gridset``: name of gridset to use (otherwise default for CRS is used)
  
