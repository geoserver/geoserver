MBTiles Output Format
============================

MBTiles WMS Output Format
--------------------------

Any WMS :ref:`wms_getmap` request can be returned in the form of a Geopackage by specifying ``format=mbtiles`` as output format (see :ref:`wms_output_formats`). \
The returned result will be an MBTiles file with a single tile layer. 

The following additional parameters can be passed on using :ref:`format_options`:
  * ``tileset_name``: name to be used for tileset in mbtiles file (default is name of layer(s)).
  * ``min_zoom``, ``max_zoom``, ``min_column``, ``max_column``, ``min_row``, ``max_row``: set the minimum and maximum zoom level, column, and rows
  * ``gridset``: name of gridset to use (otherwise default for CRS is used)
  
MBTiles WPS Process
----------------------
It is possible to generate an ``mbtiles`` file by calling the WPS process ``gs:MBTiles``. This process requires the following parameters:

  * ``layername``: Name of the input layer.
  * ``format`` : format of the final images composing the file.
  *  ``minZoom``, ``maxZoom``, ``minColumn``, ``maxColumn``, ``minRow``, ``maxRow``: *(Optional)* set the minimum and maximum zoom level, column, and rows.
  * ``boundingbox``: *(Optional)* Bounding box of the final mbtiles. If CRS is not set, the layer native one is used.
  * ``path``: *(Optional)* path of the directory where the mbtiles file is stored.
  * ``filename``: *(Optional)* name of the mbtiles file created.
  * ``bgColor``: *(Optional)* value associated to the background colour.
  * ``transparency``: *(Optional)* parameter indicating if the transparency must be present.
  * ``stylename``, ``stylepath``, ``stylebody``: *(Optional)* style to associate to the layer. Only one of these 3 parameters can be used.
  
The process returns an URL containing the path of the generated file.