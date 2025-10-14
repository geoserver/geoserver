.. _pmtiles_store_installing:

Installing the PMTiles DataStore Extension
===========================================

To install the PMTiles DataStore extension:

#. Download the **pmtiles-store** community extension from the appropriate `nightly build <https://build.geoserver.org/geoserver/>`_. The file name is called :file:`geoserver-*-pmtiles-store-plugin.zip`, where ``*`` matches the version number of GeoServer you are using.

#. Extract the contents and place all JARs in ``WEB-INF/lib``.

#. Restart GeoServer.

After installation, you will see "Protomaps PMTiles" as a new data store option when adding vector data sources.

Verifying Installation
-----------------------

To verify the extension is properly installed:

#. Log in to the GeoServer web interface
#. Navigate to :guilabel:`Stores` > :guilabel:`Add new Store`
#. Look for :guilabel:`Protomaps PMTiles` in the list of Vector Data Sources

If the PMTiles option appears, the extension has been successfully installed.
