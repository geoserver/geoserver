.. _installation_upgrade:

Upgrading existing versions
===========================

.. warning:: Be aware that some upgrades are not reversible, meaning that the data directory may be changed so that it is no longer compatible with older versions of GeoServer. See :ref:`migrating_data_directory` for more details.

The general GeoServer upgrade process is as follows:

#. Back up the current data directory. This can involve simply copying the directory to an additional place.

#. Make sure that the current data directory is external to the application (not located inside the application file structure).

#. Uninstall the old version and install the new version.

   .. note:: Alternately, you can install the new version directly over top of the old version.

#. Make sure that the new version continues to point to the same data directory used by the previous version.

Notes on upgrading specific versions
------------------------------------

GeoJSON encoding (GeoServer 2.6 and newer)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As of GeoServer 2.6, the GeoJSON produced by the WFS service no longer uses a non-standard encoding for the CRS. To reenable this behavior for compatibility purposes, set ``GEOSERVER_GEOJSON_LEGACY_CRS=true`` as a system property, context parameter, or environment variable.
