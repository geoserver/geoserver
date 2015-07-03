.. _webadmin_server_status:

Status
======
The Server Status page provides a summary of server configuration parameters and run-time status. It provides a useful diagnostic tool in a testing environment. 

.. figure:: ../images/server_status.png
   :align: center
   
   *Status Page*

Status Field Descriptions
-------------------------

The following table describes the current status indicators.

.. list-table::
   :widths: 30 70 

   * - **Option**
     - **Description**
   * - Locks
     - A WFS has the ability to lock features to prevent more than one person from updating the feature at one time.  If data is locked, edits can be performed by a single WFS editor. When the edits are posted, the locks are released and features can be edited by other WFS editors. A zero in the locks field means all locks are released. If locks is non-zero, then pressing "free locks," releases all feature locks currently help by the server, and updates the field value to zero. 
   * - Connections
     - Refers to the numbers of vector stores, in the above case 4, that were able to connect. 
   * - Memory Usage
     - The amount of memory current used by GeoServer. In the above example, 55.32 MB of memory is being used. Clicking on the "Free Memory" button,  cleans up memory marked for deletion by running the garbage collector.
   * - JVM Version
     - Denotes which version of the JVM (Java Virtual Machine) is been used to power the server. Here the JVM is Apple Inc.: 1.5.0_16.
   * - Native JAI
     - GeoServer uses `Java Advanced Imaging <https://jai.dev.java.net>`_ (JAI) framework for image rendering and coverage manipulation. When properly installed (true), JAI makes WCS and WMS performance faster and more efficient.
   * - Native JAI ImageIO
     - GeoServer uses `JAI Image IO <https://jai-imageio.dev.java.net>`_ (JAI) framework for raster data loading and image encoding. When properly installed (true), JAI Image I/O makes WCS and WMS performance faster and more efficient. 
   * - JAI Maximum Memory
     - Expresses in bytes the amount of memory available for tile cache, in this case 33325056 bytes. The JAI Maximum Memory value must be between 0.0 and {0}
   * - JAI Memory Usage
     - Run-time amount of memory is used for the tile cache. Clicking on the "Free Memory" button, clears available JAI memory by running the tile cache flushing.
   * - JAI Memory Threshold
     - Refers to the percentage, e.g. 75, of cache memory to retain during tile removal. JAI Memory Threshold value must be between 0.0 and 100.    
   * - Number of JAI Tile Threads
     - The number of parallel threads used by to scheduler to handle tiles  
   * - JAI Tile Thread Priority
     - Schedules the global tile scheduler priority. The priority value is defaults to 5, and must fall between 1 and 10.   
   * - Update Sequence
     - Refers to the number of times (60) the server configuration has been modified
   * - Resource cache
     - GeoServer does not cache data, but it does cache connection to stores, feature type definitions, external graphics, font definitions and CRS definitions as well. The "Clear" button forces those caches empty and makes GeoServer reopen the stores and re-read image and font information, as well as the custom CRS definitions stored in `${GEOSERVER_DATA_DIR}/user_projections/epsg.properties`.
   * - Configuration and catalog
     - GeoServer keeps in memory all of its configuration data. If for any reason that configuration information has become stale (e.g., an external utility has modified the configuration on disk) the "Reload" button will force GeoServer to reload all of its configuration from disk.
  

Timestamps Field Descriptions
-----------------------------

.. list-table::
   :widths: 20 80 

   * - **Option**
     - **Description**
 
   * - GeoServer
     - Currently a placeholder. Refers to the day and time of current GeoServer install.
   * - Configuration
     - Currently a placeholder. Refers to the day and time of last configuration change.
   * - XML
     - Currently a placeholder. 
     
     
   
   
   
   
   
   
   
   
   
   