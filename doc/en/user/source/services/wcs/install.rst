.. _wcs_install:

Installing the WCS 1.0 and 1.1 extensions
=========================================

GeoServer includes WCS 2.0 support in its core installation. However, WCS 1.0 and 1.1 support has been moved to an optional extension and must be installed separately if you need WCS 1.0 and 1.1.

To install the WCS 1.0 and 1.1 extensions:

#. Navigate to the :website:`GeoServer download page <download>`.

#. Find the page that matches the exact version of GeoServer you are running.

   .. warning::  Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

#. Download the needed extension:

   **WCS 1.0**:

   * |release| :download_extension:`wcs1_0`
   * |version| :nightly_extension:`wcs1_0`

   **WCS 1.1**:

   * |release| :download_extension:`wcs1_1`
   * |version| :nightly_extension:`wcs1_1`

   The download link for :guilabel:`WCS 1.0` or :guilabel:`WCS 1.1` will be in the :guilabel:`Extensions` section under :guilabel:`Other`.

#. Extract the files in these archives to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer.

After restarting, load the :ref:`web_admin`.  If the extensions loaded properly, WCS 1.0 and 1.1 support will be available alongside the core WCS 2.0 functionality.  If you encounter any issues, check the logs for errors.

.. figure:: img/wcsservice.png
   :align: center