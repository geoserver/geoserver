GeoPackage As Output
====================

GeoPackage WMS Output Format
----------------------------

Any WMS :ref:`wms_getmap` request can be returned in the form of a Geopackage by specifying ``format=geopackage`` as output format (see :ref:`wms_output_formats`). \
The returned result will be a GeoPackage file with a single tile layer. 

The following additional parameters can be passed on using :ref:`format_options`:
  * ``tileset_name``: name to be used for tileset in geopackage file (default is name of layer(s)).
  * ``min_zoom``, ``max_zoom``, ``min_column``, ``max_column``, ``min_row``, ``max_row``: set the minimum and maximum zoom level, column, and rows
  * ``gridset``: name of gridset to use (otherwise default for CRS is used)
        
GeoPackage WFS Output Format
----------------------------    

Any WFS :ref:`wfs_getfeature` request can be returned as a Geopackage by specifying ``format=geopackage`` as output format (see :ref:`wfs_output_formats`). The returned result will be a GeoPackage file with a single features layer.

GeoPackage WPS Process
----------------------

A custom GeoPackage can be created with any number of tiles and features layers using the ``GeoPackage`` WPS Process (see :ref:`wps_processes`).

The WPS process takes in one parameter: ``contents`` which is an xml schema that represents the desired output.

General outline of a ``contents`` scheme::

      <geopackage name=”mygeopackage” xmlns="http://www.opengis.net/gpkg">

      <features name=”myfeaturelayer” identifier=”L01”>
	      <description>describe the layer</description>
	      <srs> EPSG:4216 </srs>
	      <bbox>
		      <minx>-180</minx>
		      <miny>-90</miny>
		      <maxx>180</maxx>
		      <maxy>90</maxy>
	      </bbox>
	  ...
      </features>

      <tiles name=”mytileslayer” identifier=”L02”>
	      <description>describe the layer</description>
	      <srs>..</srs>
	      <bbox>..</bbox>
	      ...
      </tiles>

      </geopackage>


Each geopackage has a mandatory ``name``, which will be the name of the file (with the extension .gpkg added).
Each layer (features or tiles) has the following properties:

  * ``name`` (mandatory): the name of the layer in the geopackage;
  * ``identifier`` (optional): an identifier for the layer;
  * ``description`` (optional): a description for the layer;
  * ``srs`` ( mandatory for tiles, optional for features): coordinate reference system; for features the default is the SRS of the feature type;
  * ``bbox``  ( mandatory for tiles, optional for features): the bounding box; for features the default is the bounding box of the feature type.

Outline of the features layer::

      <features name=”myfeaturelayer” identifier=”L01”>
	      <description>..</description>
	      <srs>..</srs>
	      <bbox>..</bbox>
	      <featuretype>myfeaturetype</featuretype>
	      <propertynames>property1, property2</propertynames>
	      <filter>..</filter>
      </features>

Each features layer has the following properties: 
  * ``featuretype`` (mandatory): the feature type
  * ``propertynames`` (optional): list of comma-separated names of properties in feature type to be included (default is all properties)
  * ``filter`` (optional): any OGC filter that will be applied on features before output

Outline of the tiles layer::

      <tiles name=”mytileslayer” identifier=”L02”>
	      <description>...</description>
	      <srs>..</srs>
	      <bbox>..</bbox>	
	      <layers>layer1, layer2</styles>
	      <styles> style1, style2 </styles>
	      <sld> path/to/file.sld </sld>
	      <sldBody> .. </sldBody>	
	      <format>mime/type</format>
	      <bgcolor>ffffff</bgcolor>
	      <transparent>true</transparent>
	      <coverage>
		      <minZoom>5</minZoom>
		      <maxZoom>50</maxZoom>
		      <minColumn>6</minColumn>
		      <maxColumn>60</maxColumn>
		      <minRow>7</minRow>
		      <maxRow>70</maxRow>
	      <coverage>
	      <gridset>
		      ...
	      </gridset>
      </tiles>

Each tiles layer has the following properties: 
  * ``layers`` (mandatory): comma-separated list of layers that will be included
  * ``styles``, ``sld``, and ``sldbody`` are mutually exclusive, having one is mandatory
      * ``styles``: list of comma-separated styles to be used
      * ``sld``: path to sld style file
      * ``sldbody``: inline sld style file
  * ``format`` (optional): mime-type of image format of tiles (image/png or image/jpeg)
  * ``bgcolor`` (optional): background colour as a six-digit hexadecimal RGB value
  * ``transparent`` (optional): transparency (true or false)
  * ``coverage`` (optional)
  * ``minzoom``, ``maxzoom``, ``minColumn``, ``maxColumn``, ``minRow``, ``maxRow`` (all optional): set the minimum and maximum zoom level, column, and rows
  * ``gridset`` (optional): see following

Gridset can take on two possible (mutually exclusive) forms::

      <gridset>
	      <name>mygridset</name>
      </gridset>

where the ``name`` of a known gridset is specified; or a custom gridset may be defined as follows::

      <gridset>
	      <grids>
		      <grid>
		      <zoomlevel>1</zoomlevel>
		      <tileWidth>256</tileWidth>
		      <tileHeight>256</tileHeight>
		      <matrixWidth>4</matrixWidth>
		      <matrixHeight>4</matrixHeight>
		      <pixelXSize>0.17</pixelXSize>
		      <pixelYSize>0.17</pizelYSize>
		      </grid>
		      <grid>...</grid>
		      ...
      </grids>
      </gridset>


