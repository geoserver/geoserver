.. _community_oidc_installing:

Installing the OAUTH2/OIDC module
=================================

To install the OIDC module:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``sec-oidc`` zip archive.
   
   * |version| example: :nightly_community:`sec-oidc`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-loader-plugin.zip above).

#. Restart GeoServer.

sec-oidcInstalling the OAUTH2/OIDC module

Community modules are not yet ready for distribution with GeoServer release.
      
#. To compile the OIDC module yourself download the src bundle for your GeoServer version and compile:

   .. code-block:: bash
   
      cd src/community
      mvn install -PcommunityRelease -DskipTests
   
#. And package (from the top level geoserver directory):
 
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