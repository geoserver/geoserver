.. _community_jwtheaders_installing:

Installing JWT Headers
======================

To install the JWT Headers module:

#. To obtain the JWT Headers community module:

   * If working with a |release| nightly build, download the module: :download_community:`jwt-headers`
   
     Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
     
   * Community modules are not yet ready for distribution with GeoServer release.
      
     To compile the JWT Headers community module yourself download the src bundle for your GeoServer version and compile:

     .. code-block:: bash
     
        cd src/community
        mvn install -PcommunityRelease -DskipTests
       
     And package:
     
     .. code-block:: bash
     
        cd src/community
        mvn assembly:single -N
     
#. Place the JARs in ``WEB-INF/lib``. 

#. Restart GeoServer.


For developers;

 .. code-block:: bash
     
        cd src
        mvn install -Pjwt-headers -DskipTests