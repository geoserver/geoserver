Installing the GeoServer OGC API module
=======================================

#. Download :download_community:`ogcapi-plugin` nightly GeoServer community module `builds <https://build.geoserver.org/geoserver/main/community-latest/>`__).
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver

