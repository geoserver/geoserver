.. _mbstyle_install:

Installing the GeoServer MBStyle extension
==========================================

The MBStyle extension is listed on the GeoServer download page.

To install MBStyle extension:

#. Download the :download_extension:`mbstyle`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
   
#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   This extension includes two jars.

#. Restart GeoServer.

#. To confirm successful installation, check for a new ``MBStyle`` format option in the :ref:`styling_webadmin` editor. 