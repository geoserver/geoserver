.. _arcgrid_extension:

Installing the ArcGrid extension
================================

The ArcGrid extension adds support for publishing ESRI ArcGrid raster datasets as coverages.

ArcGrid support is provided as an optional GeoServer extension and must be installed separately.

Installing
----------

To install the ArcGrid extension:

#. Navigate to the :website:`GeoServer download page <download>`.

#. Find the page that matches the exact version of GeoServer you are running.

   .. warning::

      Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

#. Download the ArcGrid extension:

   * |release| :download_extension:`arcgrid`
   * |version| :nightly_extension:`arcgrid`

   The download link for :guilabel:`ArcGrid` will be in the :guilabel:`Extensions` section.

#. Stop GeoServer.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory of your GeoServer installation.

   .. note::

      Ensure the JAR files are placed directly in the :file:`WEB-INF/lib` directory and not in a subdirectory.

#. Restart GeoServer.

Verifying the installation
--------------------------

After restarting GeoServer:

#. Navigate to :guilabel:`Stores` -> :guilabel:`Add new Store` and confirm that :guilabel:`ArcGrid` is available under raster data sources.

#. Navigate to :guilabel:`About & Status` -> :guilabel:`Status` and confirm that the ArcGrid module is listed under :guilabel:`Modules`.

Using ArcGrid
-------------

Once installed, configuring an ArcGrid store is the same as configuring a standard ArcGrid raster store.
