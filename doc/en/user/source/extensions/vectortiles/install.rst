.. _vectortiles.install:

Installing the Vector Tiles Extension
-------------------------------------

The Vector Tiles extension is an official extension, available on the `GeoServer download <http://geoserver.org/download>`_ page.

#. Download the extension for your version of GeoServer. 

   .. warning:: Make sure to match the version of the extension to the version of GeoServer.

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

To verify that the extension was installed successfully

#. Open the :ref:`web_admin` 
#. Click :guilabel:`Layers` and select a vector layer
#. Click the :guilabel:`Tile Caching` tab
#. Scroll down to the section on :guilabel:`Tile Formats`. In addition to the standard GIF/PNG/JPEG formats, you should see the following:

   * ``application/json;type=geojson``
   * ``application/json;type=topojson``
   * ``application/vnd.mapbox-vector-tile``

   .. figure:: img/vectortiles_tileformats.png

      Vector tiles tile formats

   If you don't see these options, the extension did not install correctly.
