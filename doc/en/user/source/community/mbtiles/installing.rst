Installing the GeoServer MBTiles extension
==========================================

.. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
#. Follow the **Community Modules** link:

   Download the ``mbtiles-store-plugin`` if you wish to read MBTiles
   
   * |version| example: :nightly_community:`mbtiles-store`
   
   Download the ``mbtiles-plugin`` to also use the WMS output format generaring MBTiles and the WPS process doing the same. Make sure to install corresponding WPS extension for GeoServer instance before installing this plugin, or GeoServer won't start.
   
   * |version| example: :nightly_community:`mbtiles`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.
