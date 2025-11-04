.. _community_jwtheaders_installing:

Installing JWT Headers
======================

To install the JWT Headers module:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `jwt-headers` zip archive.
   
   * |version| example: :nightly_community:`jwt-headers`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
   
#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.


Community module is are not yet ready for distribution with GeoServer release.
      
#. To compile the JWT Headers community module yourself download the src bundle for your GeoServer version and compile:

   .. code-block:: bash
   
      cd src/community
      mvn install -PcommunityRelease -DskipTests
       
#. And package:
   
   .. code-block:: bash
   
      cd src/community
      mvn assembly:single -N
   
     
#. Place the JARs in ``WEB-INF/lib``. 

#. Restart GeoServer.

For developers;

.. code-block:: bash

   cd src
   mvn install -Pjwt-headers -DskipTests