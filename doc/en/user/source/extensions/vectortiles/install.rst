.. _vectortiles.install:

Installing the Vector Tiles Extension
-------------------------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Output Formats** extensions download **Vector Tiles**.

   * |release| example: :download_extension:`vectortiles`
   * |version| example: :nightly_extension:`vectortiles`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the archive and copy the contents into the GeoServer library :file:`WEB-INF/lib` directory located in:
   
  * GeoServer binary Jetty: :file:`<GEOSERVER_ROOT>/webapps/geoserver/WEB-INF/lib`
  * Default Tomcat deployment: :file:`<CATALINA_BASE>/webapps/geoserver/WEB-INF/lib`

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
