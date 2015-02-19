.. _extensions_importer_install:

Installing the Importer extension
=================================

The Importer extension is an official extension, available on the `GeoServer download <http://geoserver.org/download>`_ page.

#. Download the extension for your version of GeoServer. (If you see an option, select :guilabel:`Core`.)

   .. warning:: Make sure to match the version of the extension to the version of GeoServer.

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

#. To verify that the extension was installed successfully, open the :ref:`web_admin` and look for an :guilabel:`Import Data` option in the :guilabel:`Data` section on the left-side menu.

   .. figure:: images/importer_link.png

      Importer extension successfully installed.

 For additional information please see the section on :ref:`extensions_importer_using`.
