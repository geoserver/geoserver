.. _vectortiles.install:

Installing the Vector Tiles Extension
-------------------------------------

#. From the :website:`website download <download>` page, locate your release, and download:  :download_extension:`vectortiles`

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
