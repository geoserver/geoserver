.. _geopkgoutput.usage:

Using the GeoPackage Output Extension
-------------------------------------

The GeoPackage Output Extension adds support to WFS and WMS to request ``GetFeature`` and ``GetMap`` results in GeoPackage Format.

WFS
^^^


Add ``&outputFormat=geopkg`` to your request. The result will be a GeoPackage (MIME type ``application/geopackage+sqlite3``) containing the requested features.

.. code-block::  

    curl "http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeNames=ws:layername&outputFormat=geopkg" \
    -o wfs.gpkg

You can use `geopkg`, `geopackage`, or `gpkg` as the output format in the request.  Use `1.0.0`, `1.1.0`, or `2.0.0` as ``version=`` to specify which WFS version to use.

.. note::

    GeoPackages always have the ordinates in X,Y (``EAST_NORTH``) format.

WFS Output Configuration
^^^^^^^^^^^^^^^^^^^^^^^^

GeoPackage output format configuration properties are available. For information on use of configuration properties see :ref:`running in a production environment <production_config>` instructions.

geopackage.wfs.indexed
''''''''''''''''''''''

By default a spatial index is generated when generating GeoPackage output.

Use java system property ``-Dgeopackage.wfs.indexed=false`` to suppress the generation of a spatial index in generated geopackage output. 

geopackage.wfs.tempdir
''''''''''''''''''''''

The GeoPackage file format is an SQLite database which can only be managed as a file locally.  To produce a GeoPackage GeoServer makes use of a temporary file created in ``java.io.tmpdir`` location. This temporary file is removed once the response is completed.

Some container environments recommend use of a network share for their ``java.io.tmpdir`` location. This approach is not compatible with SQLite database driver which requires a local disk location and file lock.

To override the temporary file location used for GeoPackage output format file generation use property ``-Dgeopackage.wfs.tempdir=<path location>`` to provide an alternate path.

WMS
^^^

Add ``&format=geopkg`` to your request. The result will be a GeoPackage (MIME type `application/geopackage+sqlite3`) containing the requested tiles.

Using WMS 1.1.0 to access tiled image geopkg:

.. code-block::  

    curl "http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ws:layername&bbox=-123.43670607166865%2C48.3956835%2C-123.2539813%2C48.5128362547052&width=1536&height=984&srs=EPSG%3A4326&styles=&format=geopkg" \
    -o wms.gpkg

Using WMS 1.3.0 to access tiled image geopkg:

.. code-block:: 

    curl "http://localhost:8080/geoserver/wms?service=WMS&version=1.3.0&request=GetMap&layers=ws:layername&bbox=48.3956835,-123.43670607166865,48.5128362547052,-123.2539813&width=768&height=492&srs=EPSG%3A4326&styles=&format=geopkg" \
    -o wms.gpkg

You can use ``format=geopkg``, ``format=geopackage``, or ``format=gpkg`` as the output format in the request.  Use WMS ``version=1.1.0``, or ``version=1.3.0`` to specify which WMS version to use, keeping in mind axis order for ``bbox`` differences.

.. note::
    Regardless of WMS axis order used for ``bbox`` the resulting GeoPackages always have the ordinates in X,Y (``EAST_NORTH``) order as required by the specification.

WMS Format options
''''''''''''''''''

You can also add format options (``format_options=param1:value1;param2:value2;...``) to the request.   With all default values, you will get a GeoPackage with PNG tiles of multiple resolutions.  There will be a little more than 255 total tiles - all occupying the area in the request's bbox.

.. list-table:: Format Options
   :widths: auto  
   :header-rows: 1

   * - Parameter
     - Description
   * - min_zoom
     - Grid Zoom level for tiles to start.

       default: zoom level based on a single tile covering the bbox area.
   * - max_zoom
     - Grid Zoom level for tiles to end.

       default: zoom where there's >255 tiles in total in the geopkg (could be a bit more)
   * - num_zooms
     - Number of zoom levels in the geopkg.  
     
       If present then `max_zoom = min_zoom + num_zooms`
   * - format
     - Format for the image tiles in the geopkg.
     
       default: PNG
   * - tileset_name
     - Name of tile set ("layer") used in the geopkg. 
       
       default: based on the layer names given in the request ('_' separated)
   * - min_column
     - First column number (from the gridset) to use.
     
       default: use request bbox to determine which tiles to produce
   * - max_column
     - Last column number (from the gridset) to use.
     
       default: use request bbox to determine which tiles to produce
   * - min_row
     - First row number (from the gridset) to use.
     
       default: use request bbox to determine which tiles to produce
   * - max_row
     - Last row number (from the gridset) to use.
     
       default: use request bbox to determine which tiles to produce
   * - gridset
     - Name of the gridset (from GWC GridSetBroker) to uses.
     
       default: find based on request SRS
   * - flipy
     - Do NOT set.

       default: TRUE (required for GeoPackage - `The tile coordinate (0,0) always refers to the tile in the upper left corner of the tile matrix...`)
