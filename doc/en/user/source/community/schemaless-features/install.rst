Installing the Schemaless Features module
=========================================

#. Download :download_community:`schemaless-features-plugin` nightly GeoServer community module `builds <https://build.geoserver.org/geoserver/main/community-latest/>`__).
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-schemaless-features-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the ``MongoDB Schemaless`` vector source option will be available from the ``New Data Source`` page:

.. figure:: images/new-data-sources-schemaless.png

