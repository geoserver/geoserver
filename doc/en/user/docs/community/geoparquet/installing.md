# Installing the GeoParquet Extension

The GeoParquet module is a community extension that adds support for reading GeoParquet files as data stores in GeoServer.

## Prerequisites

- GeoServer 2.28 or later
- Java 17 or higher

## Installation Steps

To install the GeoParquet extension:

1.  Download the **geoparquet** community extension from the appropriate [nightly build](https://build.geoserver.org/geoserver/). The file name is called **`geoserver-*-geoparquet-plugin.zip`**, where `*` matches the version number of GeoServer you are using.
2.  Extract the contents of the zip file and place all JARs in the **`WEB-INF/lib`** directory of your GeoServer installation.
3.  Restart GeoServer to load the extension.

## Verification

After restarting, verify the installation by:

1.  Navigate to **Stores --> Add new Store** in the GeoServer web admin interface
2.  Look for **GeoParquet** in the list of available data store types under "Vector Data Sources"
3.  If the GeoParquet option appears, the extension has been successfully installed

## What Gets Installed

The GeoParquet extension provides:

- A new data store type for reading GeoParquet files
- Support for local files, HTTP/HTTPS URLs, and S3 storage
- Integration with GeoServer's standard feature serving capabilities (WFS, WMS, etc.)
- Support for Hive-partitioned datasets
- AWS credential chain authentication for secure S3 access

## Next Steps

Once installed, proceed to [Configuring GeoParquet Data Stores](configuration.md) to learn how to configure GeoParquet data stores.
