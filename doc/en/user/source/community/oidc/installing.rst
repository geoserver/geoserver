.. _community_oidc_installing:

Installing the OAUTH2/OIDC module
=================================

To install the OIDC module:

#. To obtain the OIDC community module:

   * If working with a |release| nightly build, download the module: :download_community:`sec-oidc`
   
     Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
     
   * Community modules are not yet ready for distribution with GeoServer release.
      
     To compile the OIDC module yourself download the src bundle for your GeoServer version and compile:

     .. code-block:: bash
     
        cd src/community
        mvn install -PcommunityRelease -DskipTests
       
     And package (from the top level geoserver directory):
     
     .. code-block:: bash
     
        cd ../..
        mvn -f src/community/pom.xml clean install -B -DskipTests -PcommunityRelease,assembly  -T2 -fae

     
#. Place the JARs in ``WEB-INF/lib``. 

#. Restart GeoServer.


Using with the GeoServer Docker Container
-----------------------------------------

This will run GeoServer on port 9999 and install the OIDC module.

.. code-block:: bash

   docker run -it -9999:8080 \
      --env INSTALL_EXTENSIONS=true \
      --env STABLE_EXTENSIONS="ysld,h2" \
      --env COMMUNITY_EXTENSIONS="sec-oidc-plugin" \
      docker.osgeo.org/geoserver:2.28.x

If your OIDC IDP server (i.e. keycloak) is running on `localhost`, then you should ensure that all requests to the IDP occur using the same hostname (this includes the local user's browser and GeoServer directly connecting to the IDP).  If you are running your IDP from a real host, then you do NOT have to do this;

   1. Add this to your `/etc/hosts`:

      .. code-block:: bash

         127.0.0.1       host.docker.internal
   

   2. In your GeoServer OIDC configuration, use `host.docker.internal` instead of `localhost`
   3. Access GeoServer and Keycloak with http://host.docker.internal:PORT