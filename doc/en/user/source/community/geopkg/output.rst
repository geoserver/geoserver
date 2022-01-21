GeoPackage WPS Process
======================

A custom GeoPackage can be created with any number of tiles and features layers using the ``GeoPackage`` WPS Process (see :ref:`wps_processes`).


.. warning:: While the process generates a compliant GeoPackage, some abilities like generalization, style and part of the metadata export
   are based on unofficial extensions discussed in the `Testbed 16 GeoPackage engineering report <http://docs.opengeospatial.org/per/20-019r1.html>`_.

The WPS process takes in one parameter: ``contents`` which is an XML schema that represents the desired output.

General outline of a ``contents`` scheme:

.. code-block:: xml

    <geopackage name="mygeopackage" xmlns="http://www.opengis.net/gpkg">
        <features name="myfeaturelayer" identifier="L01">
            <description>describe the layer</description>
            <srs>EPSG:4216</srs>
            <bbox>
                <minx>-180</minx>
                <miny>-90</miny>
                <maxx>180</maxx>
                <maxy>90</maxy>
            </bbox>
            <!-- ... -->
        </features>
    
        <tiles name="mytileslayer" identifier="L02">
            <description>describe the layer</description>
            <srs>..</srs>
            <bbox>..</bbox>
            <!-- ... -->
        </tiles>
    </geopackage>


Each GeoPackage has a mandatory ``name``, which will be the name of the file (with the extension .gpkg added).
Each layer (features or tiles) has the following properties:

  * ``name`` (mandatory): the name of the layer in the GeoPackage;
  * ``identifier`` (optional): an identifier for the layer;
  * ``description`` (optional): a description for the layer;
  * ``srs`` ( mandatory for tiles, optional for features): coordinate reference system; for features the default is the SRS of the feature type;
  * ``bbox``  ( mandatory for tiles, optional for features): the bounding box; for features the default is the bounding box of the feature type.

Outline of the features layer:

.. code-block:: xml

    <features name="myfeaturelayer" identifier="L01">
        <description>..</description>
        <srs>..</srs>
        <bbox>..</bbox>
        <featuretype>myfeaturetype</featuretype>
        <propertynames>property1, property2</propertynames>
        <filter>..</filter>
        <indexed>true</indexed>
        <styles>true</styles>
        <metadata>true</metadata>
        <overviews>...</overviews>
        <sort xmlns:fes="http://www.opengis.net/fes/2.0">
            <fes:SortProperty>
                <fes:ValueReference>theGeom</fes:ValueReference>
            </fes:SortProperty>
        </sort>
    </features>

Each features layer has the following properties: 
  * ``featuretype`` (mandatory): the feature type
  * ``propertynames`` (optional): list of comma-separated names of properties in feature type to be included (default is all properties)
  * ``filter`` (optional): any OGC filter that will be applied on features before output
  * ``indexed`` (optional): include spatial indexes in the output (true/false)
  * ``styles`` (optional): include styles in the output (true/false). The exported structure uses the portrayal and semantic annotation extensions, as described in  `Testbed 16 E/R <http://docs.opengeospatial.org/per/20-019r1.html#_portrayal>`_
  * ``metadata`` (optional): embed metadata referred by the layer metadata links into the GeoPackage (true/false). The base metadata tables are filled with   contents, while semantic annotations might be used to add extra information about the metadata itself.
  * ``overviews`` (optional): adds overview tables that can speed up rendering. See more at :ref:`overviews`
  * ``sort`` (optional): a filter encoding ``fes:SortByType`` which allows sorting the table contents on one or more attributes. If the chosen attribute
    is a geometry, the table will be sorted on its GeoHash, `improving access locality <http://docs.opengeospatial.org/per/20-019r1.html#record_sorting>`_
    when using spatial indexes.

Outline of the tiles layer:

.. code-block:: xml

    <tiles name="mytileslayer" identifier="L02">
        <description>...</description>
        <srs>..</srs>
        <bbox>..</bbox>
        <layers>layer1, layer2</styles>
        <styles>style1, style2</styles>
        <sld>path/to/file.sld</sld>
        <sldBody>..</sldBody>
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
        </coverage>
        <gridset>
            ...
        </gridset>
        <parameters>
          <parameter name="...">value</parameter>
        <parameters>
    </tiles>

Each tiles layer has the following properties: 
  * ``layers`` (mandatory): comma-separated list of layers that will be included
  * ``styles``, ``sld``, and ``sldbody`` are mutually exclusive, having one is mandatory
      * ``styles``: list of comma-separated styles to be used
      * ``sld``: path to SLD style file
      * ``sldbody``: inline SLD style file
  * ``format`` (optional): mime-type of image format of tiles (image/png or image/jpeg)
  * ``bgcolor`` (optional): background colour as a six-digit hexadecimal RGB value
  * ``transparent`` (optional): transparency (true or false)
  * ``coverage`` (optional)
  * ``minzoom``, ``maxzoom``, ``minColumn``, ``maxColumn``, ``minRow``, ``maxRow`` (all optional): set the minimum and maximum zoom level, column, and rows
  * ``gridset`` (optional): see below
  * ``parameters`` (optional): list of other parameters that can be used in a GetMap to produce tiles (open to all GeoServer vendor parameters)

Gridset can take on two possible (mutually exclusive) forms:

.. code-block:: xml

      <gridset>
          <name>mygridset</name>
      </gridset>

where the ``name`` of a known gridset is specified; or a custom gridset may be defined as follows:

.. code-block:: xml

    <gridset>
        <grids>
            <grid>
                <zoomlevel>1</zoomlevel>
                <tileWidth>256</tileWidth>
                <tileHeight>256</tileHeight>
                <matrixWidth>4</matrixWidth>
                <matrixHeight>4</matrixHeight>
                <pixelXSize>0.17</pixelXSize>
                <pixelYSize>0.17</pixelYSize>
            </grid>
            <grid>...</grid>
            <!-- ... -->
        </grids>
    </gridset>

..  _overviews:

Creating generalized tables
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The process can create generalized tables, as described in `Testbed 16 generalized tables
extension <http://docs.opengeospatial.org/per/20-019r1.html#im_generalized_tables_extension>`_.

Generalized tables are sidecar tables that typically contain less records than the original
table, with the option to also generalize their geometry. These are created by adding
a list of ``overview`` directives in a feature layer description, each one containing:

    * ``name`` (mandatory): the generalized table name
    * ``distance`` (optional): the generalization distance to create simplified geometries
    * ``scaleDenominator``: the scale denominator at which the table starts being used, in preference to the original table, and other tables with a lower scale denominator value
    * ``filter`` (optional): an OGC filter removing features that are not meant to be rendered at the target scale denominator

Here is an example:

.. code-block:: xml

    <features name="woodland" identifier="woodland">
      <description>woodland</description>
      <srs>EPSG:27700</srs>
      <featuretype>oszoom:woodland</featuretype>
      <indexed>true</indexed>
      <styles>true</styles>
      <overviews>
        <overview>
          <name>woodland_g1</name>
          <scaleDenominator>80000</scaleDenominator>
          <filter xmlns:fes="http://www.opengis.net/fes/2.0">
            <fes:Or>
              <fes:PropertyIsEqualTo>
                <fes:ValueReference>type</fes:ValueReference>
                <fes:Literal>National</fes:Literal>
              </fes:PropertyIsEqualTo>
              <fes:PropertyIsEqualTo>
                <fes:ValueReference>type</fes:ValueReference>
                <fes:Literal>Regional</fes:Literal>
              </fes:PropertyIsEqualTo>
            </fes:Or>
          </filter>
        </overview>
        <overview>
          <name>woodland_g2</name>
          <scaleDenominator>320000</scaleDenominator>
          <filter xmlns:fes="http://www.opengis.net/fes/2.0">
            <fes:PropertyIsEqualTo>
              <fes:ValueReference>type</fes:ValueReference>
              <fes:Literal>National</fes:Literal>
            </fes:PropertyIsEqualTo>
          </filter>
        </overview>
      </overviews>
    </features>
