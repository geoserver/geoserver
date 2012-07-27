.. _webadmin_gwc:

GeoWebCache Settings
====================

The GeoWebCache Settings page in the Server menu in the :ref:`web_admin` shows some configuration options for GeoWebCache, a tile server that comes embedded by default inside GeoServer.  For more information about this embedded version, please see the section on :ref:`geowebcache`.

.. figure:: img/gwcsettings.png
   :align: center

Enable direct WMS integration
-----------------------------

GeoWebCache acts as a proxy between GeoServer and map client.  By default, GeoWebCache has a separate endpoint from the GeoServer WMS.  (See the section on :ref:`gwc_using` for more details.)

Enabling direct WMS integration allows WMS requests served through GeoServer to be cached as if they were received and processed by GeoWebCache.  This yields the flexibility of a WMS with the speed of a tile server.  See the section on :ref:`gwc_using` for more details about this feature.

Disk quota
----------

This section manages the disk usage for tiles saved with GeoWebCache.

By default, disk usage with GeoWebCache is unbounded, regardless of integration with the GeoServer WMS, so every tile served from GeoWebCache will be stored in the cache directory (typically the :file:`gwc` directory inside the data directory).  When direct WMS integration is enabled but disk quotas not enabled, every tile that is served through both the GeoServer WMS and GeoWebCache will be stored in the cache directory, which could cause disk capacity issues.  Setting a disk quota allows disk usage to be constrained.

.. list-table::
   :widths: 30 15 55
   :header-rows: 1

   * - Option
     - Default value
     - Description
   * - :guilabel:`Enable Disk Quota limits`
     - Off
     - Turns on the disk quota.  When disabled, the cache directory will grow unbounded.  When enabled, the disk quota will be set according to the options below.
   * - :guilabel:`Compute cache usage based on a disk block size of`
     - 4096 bytes
     - This field should be set equal to the disk block size of the storage medium where the cache is located.
   * - :guilabel:`Check if the cache disk quota is exceeded every`
     - 10 seconds
     - Time interval at which the cache is polled.  Smaller values (more frequent polling) will slightly increase disk activity, but larger values (less frequent polling) might cause the disk quota to be temporarily exceeded.
   * - :guilabel:`Set maximum tile cache size`
     - 100 MiB (Mebibytes)
     - The maximum size for the cache.  When this value is exceeded and the cache is polled, tiles will be removed according to the policy choice listed below.  Note that the unit options are **mebibytes** (approx. 1.05MB), **gibibytes** (approx. 1.07GB), and **tebibytes** (approx. 1.10TB).
   * - :guilabel:`When forcing disk quota limits, remove first tiles that are`
     - Least Frequently Used
     - Sets the policy for tile removal when the disk quota is exceeded.  Options are **Least Frequently Used** (removes tiles based on how often the tile was accessed) or **Least Recently Used** (removes tiles based on date of last access).

.. note:: See the `GeoWebCache documentation <http://geowebcache.org/docs>`_ for more about disk quotas.

When finished making changes, click :guilabel:`Submit`.

This section also shows how much disk space is being used compared to the disk quota size, as well as the last time (if any) the quota was reached.


Links
-----

This page contains links to the embedded GWC homepage (containing runtime statistics and status updates) and :ref:`gwc_demo` where you can view all layers known to GeoWebCache and reload configuration.

