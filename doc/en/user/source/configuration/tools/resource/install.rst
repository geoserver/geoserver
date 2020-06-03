.. _web_resource_install:

Installing the GeoServer Web Resource extension
===============================================

The :guilabel:`Resource Brower` tool is provided by the web-resource extension is listed on the GeoServer download page.

To install web-resource extension:

#. Download the :file:`geoserver-2.17-RC-web-resource-plugin.zip` from `GeoServer Download <http://geoserver.org/download/>`__ page.
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
   
#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   This extension includes two jars.

#. Restart GeoServer.

#. To confirm successful installation, navigate to :menuselection:`Tools` page and confirm the availability of :guilabel:`Resource Browser` tool.