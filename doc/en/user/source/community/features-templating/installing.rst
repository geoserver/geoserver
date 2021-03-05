Installing the GeoServer FEATURES-TEMPLATING extension
======================================================
  
 #. Download the extension from the `nightly GeoServer community module builds <https://build.geoserver.org/geoserver/main/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

 #. The full package requires the OGC API - Features service to be available. If the server does not include it, the 
    jar ``gs-features-templating-ogcapi-<version>.jar`` should be removed from ``WEB-INF/lib``
