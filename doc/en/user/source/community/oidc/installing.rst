.. _community_oidc_installing:

Installing the OAUTH2/OIDC module
=================================

To install the OIDC module:

#. To obtain the OIDC community module:

   * If working with a |release| nightly build, download the module: :download_community:`sec-oidc`
   
     Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
     
   * Community modules are not yet ready for distribution with GeoServer release.
      
     To compile the keycloak community module yourself download the src bundle for your GeoServer version and compile:

     .. code-block:: bash
     
        cd src/community
        mvn install -PcommunityRelease -DskipTests
       
     And package:
     
     .. code-block:: bash
     
        cd src/community
        mvn assembly:single -N
     
#. Place the JARs in ``WEB-INF/lib``. 

#. Restart GeoServer.
