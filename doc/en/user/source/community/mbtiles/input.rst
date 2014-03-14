MBTiles Data Store
==================
 
Adding an MBTiles Mosaic Data Store
-----------------------------------

When the extension has been installed, :guilabel:MBTiles` will be an option in the :guilabel:`Raster Data Sources` list when creating a new data store.

.. figure:: images/mbtilescreate.png
   :align: center

   *MBTiles in the list of raster data stores*

.. figure:: images/mbtilesconfigure.png
   :align: center

   *Configuring an MBTiles data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - Name of the workspace to contain the MBTiles Mosaic store. This will also be the prefix of the raster layers created from the store.
   * - ``Data Source Name``
     - Name of the MBTiles Store as it will be known to GeoServer. This can be different from the filename. 
   * - ``Description``
     - A full free-form description of the MBTiles store.
   * - ``Enabled``
     -  If checked, it enables the store. If unchecked (disabled), no data in the GeoPackage Mosaic Store will be served from GeoServer.
   * - ``URL``
     - Location of the MBTiles file. This can be an absolute path (such as :file:`file:C:\\Data\\landbase.mbtiles`) or a path relative to GeoServer's data directory (such as :file:`file:data/landbase.mbtiles`).
 