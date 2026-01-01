.. _image_extension:

Installing the Image extension
==============================

The Image extension adds support for publishing raster images accompanied by an ESRI world file (a six-line text file used to georeference an image). World files are commonly named with a format-specific extension such as :file:`.pgw` (PNG), :file:`.jgw` (JPEG), or :file:`.tfw` (TIFF); a generic :file:`.wld` extension may also be used.

In GeoServer, this store type is referred to as :guilabel:`WorldImage`.

Installing
----------

To install the Image extension:

#. Navigate to the :website:`GeoServer download page <download>`.

#. Find the page that matches the exact version of GeoServer you are running.

   .. warning::

      Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

#. Download the Image extension:

   * |release| :download_extension:`image`
   * |version| :nightly_extension:`image`

   The download link for :guilabel:`Image` will be in the :guilabel:`Extensions` section.

#. Stop GeoServer.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory of your GeoServer installation.

   .. note::

      Ensure the JAR files are placed directly in the :file:`WEB-INF/lib` directory and not in a subdirectory.

#. Restart GeoServer.

Verifying the installation
--------------------------

After restarting GeoServer:

#. Navigate to :guilabel:`Stores` -> :guilabel:`Add new Store` and confirm that :guilabel:`WorldImage` is available under raster data sources.

#. Navigate to :guilabel:`About & Status` -> :guilabel:`Status` and confirm that the Image module is listed under :guilabel:`Modules`.

Using WorldImage
----------------

Once installed, configuring a WorldImage store is the same as configuring a standard WorldImage raster store.
