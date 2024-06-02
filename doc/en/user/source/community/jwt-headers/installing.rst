.. _community_jwtheaders_installing:

Installing JWT Headers
======================

To install the JWT Headers module:

#. If working with a |version|.x nightly build, download the module: :download_community:`jwt-headers`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |version|.x above).

#. Place the JARs in ``WEB-INF/lib``. 

#. Restart GeoServer.

.. note:: 

   Community modules are not yet ready for distribution with GeoServer |release| release.
      
   To compile the JWT Headers community module yourself download the src bundle for GeoServer |release| and compile:

   .. code-block:: bash
     
      cd src/community
      mvn install -PcommunityRelease -DskipTests
       
   And package:
     
   .. code-block:: bash
   
      cd src/community
      mvn assembly:single -N

For developers;

.. code-block:: bash
    
       cd src
       mvn install -Pjwt-headers -DskipTests