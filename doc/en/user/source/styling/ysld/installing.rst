.. _ysld_install:

Installing the GeoServer YSLD extension
=======================================

The YSLD extension is listed on the GeoServer download page.

To install:

#. Download the :download_extension:`ysld`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

#. To confirm successful installation, check for a new YSLD entry in the :ref:`styling_webadmin` editor. 