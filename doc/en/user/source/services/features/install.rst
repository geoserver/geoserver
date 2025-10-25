OGC API Features Installation
-----------------------------

Installing OGC API Features extension
'''''''''''''''''''''''''''''''''''''

#. Download the OGC API Features zip:

   * |release| :download_extension:`ogcapi-features`
   * |version| :nightly_extension:`ogcapi-features`
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-features-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the feature services is listed on the welcome page: http://localhost:8080/geoserver/

   .. figure:: img/welcome-ogc-api-features.png
     
      GeoServer Welcome Page OGC API - Features

#. The feature service is available at: http://localhost:8080/geoserver/ogc/features/v1

Docker use of OGC API Features extension
''''''''''''''''''''''''''''''''''''''''

#. The Docker image supports the use of OGC API Feature:

   .. only:: not snapshot
   
      .. parsed-literal::

         docker pull docker.osgeo.org/geoserver:|release|

   .. only:: snapshot
   
      .. parsed-literal::
   
         docker pull docker.osgeo.org/geoserver:|version|.x

#. To run with OGC API Features:

   .. only:: not snapshot
   
      .. parsed-literal::
      
         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="ogcapi-features" \\
           docker.osgeo.org/geoserver:|release|
   
   .. only:: snapshot
   
      .. parsed-literal::
   
         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="ogcapi-features" \\
           docker.osgeo.org/geoserver:|version|.x

#. The feature service is listed on the welcome page: http://localhost:8080/geoserver/

   .. figure:: img/welcome-ogc-api-features.png
     
      GeoServer Welcome Page OGC API - Features

#. The feature service is available at: http://localhost:8080/geoserver/ogc/features/v1