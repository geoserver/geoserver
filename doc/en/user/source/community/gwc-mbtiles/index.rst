.. _community_gwc_mbtiles:

GWC MBTiles layer plugin
========================

This plugin allows adding a GWC MBTiles Layer to GeoServer's cached Tile Layers configuration so that GeoServer can load cached tiles from ready-to-use MBTiles.

Installation
------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `gwc-mbtiles` zip archive.
   
   * |version| example: :nightly_community:`gwc-mbtiles`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.
   
#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.

Configuring a MBTilesLayer
--------------------------
The configuration of a new MBTilesLayer can be done by editing the geowebcache configuration file located in :file:`<data_dir>/gwc/geowebcache.xml`

Locate the `layers` section of the config or add it if missing.

Then define a new `mbTilesLayer` node for each new MBTilesLayer you want to add. A GWC configuration containing a configured MBTiles Layer would look like this:


.. code-block:: xml

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


A few note on the above configuration elements of an mbtilesLayer definition:

* `tilesPath` (mandatory) is the path to the MBTiles file containing the tiles.
* `tileSize` is the size of the tiles stored on the MBTiles file. When not set, it will be defaulted to 256.
* `name` (optional) represents the name to be assigned to the layer. If not specified, the name field of the metadata table stored in the MBTiles file will be used. Make sure to define it in case the MBTiles metadata is missing it.
* `metaInformation` with `title` and `description` are optional tags. They will be exposed in the capabilities document when available.

