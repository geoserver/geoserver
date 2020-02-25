.. _config_serverstatus:

Status
======
The Server Status page has two tabs to summarize the current status of GeoServer. The Status tab provides a summary of server configuration parameters and run-time status. The modules tab provides the status of the various modules installed on the server. This page provides a useful diagnostic tool in a testing environment. 

Server Status
-------------

.. figure:: img/server_status.png
   
   Status Page (default tab)

Status Field Descriptions
^^^^^^^^^^^^^^^^^^^^^^^^^

The following table describes the current status indicators.

.. list-table::
   :widths: 30 70 
   :header-rows: 1

   * - Option
     - Description
   * - Data directory
     - Shows the path to the GeoServer data directory (GEOSERVER_DATA_DIR property).
   * - Locks
     - A WFS has the ability to lock features to prevent more than one person from updating the feature at one time.  If data is locked, edits can be performed by a single WFS editor. When the edits are posted, the locks are released and features can be edited by other WFS editors. A zero in the locks field means all locks are released. If locks is non-zero, then pressing "free locks," releases all feature locks currently help by the server, and updates the field value to zero. 
   * - Connections
     - Refers to the numbers of vector stores, in the above case 4, that were able to connect. 
   * - Memory Usage
     - The amount of memory current used by GeoServer. In the above example, 118 MB of memory out of a total of 910 MB is being used. Clicking on the "Free Memory" button,  cleans up memory marked for deletion by running the garbage collector.
   * - JVM Version
     - Denotes which version of the JVM (Java Virtual Machine) is been used to power the server. Here the JVM is Oracle Corporation.: 1.8.0_60.
   * - Java Rendering Engine
     - Shows the rendering engine used for vector operations.
   * - Available Fonts
     - Shows the number of fonts available. Selecting the link will show the full list.
   * - Native JAI
     - GeoServer uses `Java Advanced Imaging <https://jai.dev.java.net>`_ (JAI) framework for image rendering and coverage manipulation. When properly installed (true), JAI makes WCS and WMS performance faster and more efficient.
   * - Native JAI ImageIO
     - GeoServer uses `JAI Image IO <https://jai-imageio.dev.java.net>`_ (JAI) framework for raster data loading and image encoding. When properly installed (true), JAI Image I/O makes WCS and WMS performance faster and more efficient. 
   * - JAI Maximum Memory
     - Expresses in bytes the amount of memory available for tile cache, in this case 455 Mbytes.
   * - JAI Memory Usage
     - Run-time amount of memory is used for the tile cache. Clicking on the "Free Memory" button, clears available JAI memory by running the tile cache flushing.
   * - JAI Memory Threshold
     - Refers to the percentage, e.g. 75, of cache memory to retain during tile removal.
   * - Number of JAI Tile Threads
     - The number of parallel threads used by the scheduler to handle tiles.
   * - JAI Tile Thread Priority
     - Schedules the global tile scheduler priority. The priority value defaults to 5, and must fall between 1 and 10.
   * - ThreadPoolExecutor Core Pool Size
     - Number of threads that the ThreadPoolExecutor will create. This is underlying Java runtime functionality - see the Java documentation for ThreadPoolExecutor for more information.
   * - ThreadPoolExecutor Max Pool Size
     - Maximum number of threads that the ThreadPoolExecutor will create. This is underlying Java runtime functionality - see the Java documentation for ThreadPoolExecutor for more information.
   * - ThreadPoolExecutor Keep Alive Time (ms)
     - Timeout for threads to be terminated if they are idle and more than the core pool number exist. This is underlying Java runtime functionality - see the Java documentation for ThreadPoolExecutor for more information.
   * - Update Sequence
     - Refers to the number of times (426) the server configuration has been modified.
   * - Resource cache
     - GeoServer does not cache data, but it does cache connection to stores, feature type definitions, external graphics, font definitions and CRS definitions as well. The "Clear" button forces those caches empty and makes GeoServer reopen the stores and re-read image and font information, as well as the custom CRS definitions stored in `${GEOSERVER_DATA_DIR}/user_projections/epsg.properties`.
   * - Configuration and catalog
     - GeoServer keeps in memory all of its configuration data. If for any reason that configuration information has become stale (e.g., an external utility has modified the configuration on disk) the "Reload" button will force GeoServer to reload all of its configuration from disk.
  

Module Status
-------------

The modules tab provides a summary of the status of all installed modules in the running server. 

.. figure:: img/module_status.png
   
   Module Status
   
Field Descriptions
^^^^^^^^^^^^^^^^^^

.. list-table::
   :widths: 20 80
   :header-rows: 1
   
   * - Module Name
     - The human readable name of the module, this links to a popup containing the full details and messages of the module
   * - Module ID
     - The internal package name of the module
   * - Available?
     - Whether the module is available to GeoServer
   * - Enabled?
     - Whether the module is enabled in the current GeoServer configuration
   * - Component
     - (Optional) Optional component identifier within the module
   * - Version
     - (Optional) The version of the installed module
   * - Message (popup)
     - (Optional) status message such as what Java rendering engine is in use, or the library path if the module/driver is unavailable

.. figure:: img/module_popup.png

   Module Status popup
     

System Status
-------------

System Status adds some extra information about the system in the GeoServer status page in a tab named ``System Status``
and make that info queryable through GeoServer REST interface. This info should allow an administrator to get a quick understanding about the status of the GeoServer instance. 

`Library OSHI <https://github.com/oshi/oshi/>`_ is used to retrieving system-level information without depending on native libraries or DLLs, relying solely on `Apache JNA <https://github.com/java-native-access/jna/>`_. Major operating systems (Linux, Windows and MacOX) are supported out of the box.

The available system information is:

.. list-table::
   :widths: 30 20 50

   * - **Info**
     - **Example**
     - **Description**
   * - Operating system
     - Linux Mint 18
     - Name of the operating system and the used version
   * - Uptime
     - 08:34:50
     - Up time of the system
   * - System average load 1 minute
     - 0.90
     - System average load for the last minute
   * - System average load 5 minutes
     - 1.12
     - System average load for the last five minute
   * - System average load 15 minute
     - 0.68
     - System average load for the last fifteen minute
   * - Number of physical CPUs
     - 4
     - Number of physical CPUs / cores available
   * - Number of logical CPUs
     - 8
     - Number of logical CPUs / cores available
   * - Number of running process
     - 316
     - Total number of process running in the system
   * - Number of running threads
     - 1094
     - Total number of threads running in the system
   * - CPU load average
     - 4.12 %
     - Average load of the CPU in the last second
   * - CPU * load
     - 11.43 %
     - Load of a specific core in the last second
   * - Used physical memory
     - 31.58 %
     - Percentage of the system memory used
   * - Total physical memory
     - 31.4 GiB
     - System total memory
   * - Free physical memory
     - 21.4 GiB
     - System memory available for use
   * - Used swap memory
     - 0.00%
     - Percentage of swap memory used
   * - Total swap memory
     - 32.0 GiB
     - System total swap memory
   * - Free swap memory
     - 32.0 GiB
     - Free swap memory
   * - File system usage
     - 65.47 %
     - File system usage taking in account all partitions
   * - Partition * used space
     - 54.8 %
     - Percentage of space used in a specific partition
   * - Partition * total space
     - 338.9 GiB
     - Total space of a specific partition
   * - Partition * free space
     - 117.0 GiB
     - Free space on a specific partition
   * - Network interfaces send
     - 42.0 MiB
     - Data send through all the available network interfaces
   * - Network interfaces received
     - 700.4 MiB
     - Data received through all the available network interfaces
   * - Network interface * send
     - 25.0 MiB
     - Data send through a specific network interface
   * - Network interface * received
     - 250.4 MiB
     - Data received through a specific network interface
   * - CPU temperature
     - 52.00 ºC
     - CPU temperature
   * - CPU voltage
     - 1.5 V
     - CPU voltage
   * - GeoServer CPU usage
     - 3.5 %
     - Percentage of CPU used by GeoServer in the last second
   * - GeoServer threads
     - 49
     - Number of threads created by GeoServer
   * - GeoServer JVM memory usage
     - 5.83 %
     - Percentage of the JVM memory used by GeoServer

If some information is not available the special term ``NOT AVAILABLE`` will appear. Values will be automatically converted to best human readable unit. 


Usage
^^^^^

The system information will be available in the GeoServer status page in the ``System status`` tab (the following image only shows part of the available system information):

.. figure:: img/gui.png
   :align: center

|

If the ``System status`` tab is not present, it means that the plugin was not installed correctly. The ``System status`` tab content will be refreshed automatically every second.  

REST interface
^^^^^^^^^^^^^^

It is possible to request the available system information (monitoring data) through GeoServer REST API. The supported formats are XML, JSON and HTML. 

The available REST endpoints are: ::

    /geoserver/rest/about/system-status
    
    /geoserver/rest/about/system-status.json

    /geoserver/rest/about/system-status.xml

    /geoserver/rest/about/system-status.html

The HTML representation of the system data is equal to the ``System status`` tab representation:

 .. figure:: img/resthtml.png
   :align: center

|

The XML and JSON representations are quite similar, for each system information the following attributes will be available:

.. list-table::
   :widths: 40 60

   * - **Name**
     - **Description**
   * - name
     - name of the metric
   * - available
     - TRUE if the system information value is available
   * - description
     - description of this system information
   * - unit
     - unit of the system information, can be empty 
   * - category
     - category of this system information
   * - priority
     - this value can be used to render the metrics in a predefined order
   * - identifier
     - identifies the resource associated with the metric, e.g. file partition name

Example of XML representation: ::

	<metrics>
    <metric>
      <value>99614720</value>
      <available>true</available>
      <description>Partition [/dev/nvme0n1p2] total space</description>
      <name>PARTITION_TOTAL</name>
      <unit>bytes</unit>
      <category>FILE_SYSTEM</category>
      <identifier>/dev/nvme0n1p2</identifier>
      <priority>507</priority>
    </metric>
		(...)

Example of JSON representation: ::

	{
	    "metrics": {
	        "metric": [
	            {
                  "available": true,
                  "category": "FILE_SYSTEM",
                  "description": "Partition [/dev/nvme0n1p2] total space",
                  "identifier": "/dev/nvme0n1p2",
                  "name": "PARTITION_TOTAL",
                  "priority": 507,
                  "unit": "bytes",
                  "value": 99614720
              },
	            (...)



