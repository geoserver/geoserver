# Installing the PMTiles DataStore Extension

To install the PMTiles DataStore extension:

1.  Download the **pmtiles-store** community extension from the appropriate [nightly build](https://build.geoserver.org/geoserver/). The file name is called **`geoserver-*-pmtiles-store-plugin.zip`**, where `*` matches the version number of GeoServer you are using.
2.  Extract the contents and place all JARs in `WEB-INF/lib`.
3.  Restart GeoServer.

After installation, you will see "Protomaps PMTiles" as a new data store option when adding vector data sources.

## Verifying Installation

To verify the extension is properly installed:

1.  Log in to the GeoServer web interface
2.  Navigate to **Stores** > **Add new Store**
3.  Look for **Protomaps PMTiles** in the list of Vector Data Sources

If the PMTiles option appears, the extension has been successfully installed.
