# Installing the DuckDB Extension

!!! warning
    Make sure to match the module version with your GeoServer version.

1. Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact GeoServer version in use.

2. Visit the [download page](https://geoserver.org/download/), switch to the **Development** tab, and locate the nightly build matching your GeoServer version.

3. Follow the **Community Modules** link and download the DuckDB module.

   The website lists active nightly builds to provide feedback to developers. You may also [browse the build server](https://build.geoserver.org/geoserver/) for earlier branches.

4. Extract the downloaded archive into `WEB-INF/lib` of your GeoServer installation.

5. Restart GeoServer.

!!! note
    If the GeoParquet community module is already installed, the DuckDB JDBC driver may already be present.
    You still need to install the DuckDB extension to register the DuckDB store type in GeoServer.

## Verification

After restart:

1. Open **Stores > Add new Store**.
2. Confirm **DuckDB** appears under vector data sources.