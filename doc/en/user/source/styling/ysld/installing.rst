.. _ysld_install:

YSLD Extension Installation
===========================

Installing YSLD extension
'''''''''''''''''''''''''

The YSLD extension is listed on the GeoServer download page.

To install:

#. Download:
   
   * |release| :download_extension:`ysld`
   * |version| :nightly_extension:`ysld`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

#. To confirm successful installation, check for a new YSLD entry in the :ref:`styling_webadmin` editor.

Docker use of YSLD extension
''''''''''''''''''''''''''''

#. The Docker image supports the use of YSLD extension

   .. only:: not snapshot
   
      .. parsed-literal::

         docker pull docker.osgeo.org/geoserver:|release|

   .. only:: snapshot
   
      .. parsed-literal::
   
         docker pull docker.osgeo.org/geoserver:|version|.x

#. To run with YSLD extension:

   .. only:: not snapshot
   
      .. parsed-literal::
      
         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="ysld" \\
           docker.osgeo.org/geoserver:|release|
   
   .. only:: snapshot
   
      .. parsed-literal::
   
         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="ysld" \\
           docker.osgeo.org/geoserver:|version|.x

#. To confirm successful installation, check for a new YSLD entry in the :ref:`styling_webadmin` editor.
