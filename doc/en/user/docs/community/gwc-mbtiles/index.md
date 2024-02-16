# GWC MBTiles layer plugin {: #community_gwc_mbtiles }

This plugin allows adding a GWC MBTiles Layer to GeoServer's cached Tile Layers configuration so that GeoServer can load cached tiles from ready-to-use MBTiles.

## Installation

As a community module, make sure you have downloaded the `gwc-mbtiles`{.interpreted-text role="download_extension"}

To install the module, unpack the zip file contents into the GeoServer `WEB-INF/lib` directory and restart GeoServer.

## Configuring a MBTilesLayer

The configuration of a new MBTilesLayer can be done by editing the geowebcache configuration file located in **`<data_dir>/gwc/geowebcache.xml`**

Locate the `rs` section of the config or add it if missing.

Then define a new `er` node for each new MBTilesLayer you want to add. A GWC configuration containing a configured MBTiles Layer would look like this:

``` xml
<gwcConfiguration>
  <!-- ... -->
  <layers>
    <!-- Other layer definitions may be here -->
    <mbtilesLayer>
      <tilesPath>D:\Data\mbtiles\countries-raster.mbtiles</tilesPath>
      <tileSize>256</tileSize>
      <name>countries</name>
      <metaInformation>
        <title>Countries</title>
        <description>Raster Layer with Countries</description>
      </metaInformation>
    </mbtilesLayer>
    <!-- Other layer definitions may be here -->
  </layers>
</gwcConfiguration>
```

A few note on the above configuration elements of an mbtilesLayer definition:

-   `th` (mandatory) is the path to the MBTiles file containing the tiles.
-   `ze` is the size of the tiles stored on the MBTiles file. When not set, it will be defaulted to 256.
-   `me` (optional) represents the name to be assigned to the layer. If not specified, the name field of the metadata table stored in the MBTiles file will be used. Make sure to define it in case the MBTiles metadata is missing it.
-   `on` with `le` and `on` are optional tags. They will be exposed in the capabilities document when available.
