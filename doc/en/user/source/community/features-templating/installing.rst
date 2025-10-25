Installing the GeoServer FEATURES-TEMPLATING extension
======================================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `features-templating` zip archive.
   
   * |version| example: :nightly_community:`features-templating`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. The full package requires the OGC API - Features service to be available. If the server does not include it, the 
   jar ``gs-features-templating-ogcapi-<version>.jar`` should be removed from ``WEB-INF/lib``

#. Restart GeoServer.
