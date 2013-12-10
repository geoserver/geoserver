.. _webadmin_tilecaching_defaults:

Caching defaults
================

The Caching Defaults page shows the global configuration options for the tile caching functionality in GeoServer, an embedded GeoWebCache.

.. note:: For more information about this embedded version, please see the section on :ref:`geowebcache`.

GWC Provided Services
---------------------

In addition to the GeoServer endpoints, GeoWebCache provides other endpoints for OGC services. For example, the GeoServer WMS endpoint is available at::

  http://GEOSERVER_URL/wms?...

The GeoWebCache WMS endpoint is::

  http://GEOSERVER_URL/gwc/service/wms?...

.. figure:: img/defaults_services.png

   *Provided services*

The following settings describe the different services that can be enabled with GeoWebCache.

Enable direct integration with GeoServer WMS
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Direct integration allows WMS requests served through GeoServer to be cached as if they were received and processed by GeoWebCache. This provides all the advantages of using a tile server while still employing the more-flexible GeoServer WMS as a fallback. See the section on :ref:`gwc_using` for more details about this feature.

With direct integration, tile caching is enabled for all standard WMS requests that contain the ``tiled=true`` parameter and conform to all required parameters.

This setting is disabled by default. When enabling this option, it is a good idea to also turn on :ref:`webadmin_tilecaching_diskquotas` as well, to prevent unbounded growth of the stored tiles.

Enable WMS-C Service
~~~~~~~~~~~~~~~~~~~~

Enables the Cached Web Map Service (WMS-C) service. When this setting is enabled, GeoWebCache will respond to its own WMS-C endpoint::

  http://GEOSERVER_URL/gwc/service/wms?SERVICE=WMS&VERSION=1.1.1&TILED=true&...

When the service is disabled, calls to the capabilities document will return a ``Service is disabled`` message.

Enable TMS Service
~~~~~~~~~~~~~~~~~~

Enables the Tiled Map Service (TMS) endpoint in GeoWebCache. With the TMS service, GeoWebCache will respond to its own TMS endpoint::

  http://GEOSERVER/URL/gwc/service/tms/1.0.0

When the service is disabled, calls to the capabilities document will return a ``Service is disabled`` message.

Enable WMTS Service
~~~~~~~~~~~~~~~~~~~

Enables the Web Map Tiled Service (WMTS) endpoint in GeoWebCache. When this setting is enabled, GeoWebCache will respond to its own WMTS endpoint::

  http://GEOSERVER/URL/gwc/service/wmts?...

When the service is disabled, calls to the capabilities document will return a ``Service is disabled`` message.

Enable Data Security
~~~~~~~~~~~~~~~~~~~~

Enables the :ref:`gwc_data_security` in the embedded GeoWebCache.

Default Caching Options for GeoServer Layers
--------------------------------------------

This section describes the configuration of the various defaults and other global options for the tile cache in GeoServer.

.. figure:: img/defaults_options.png
   :align: center

   *Default caching options*

Automatically configure a GeoWebCache layer for each new layer or layer group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This setting, enabled by default, determines how layers in GeoServer are handled via the embedded GeoWebCache. When this setting is enabled, an entry in the GeoWebCache layer listing will be created whenever a new layer or layer group is published in GeoServer. Use this setting to keep the GeoWebCache catalog in sync. (This is enabled by default.)

Automatically cache non-default styles
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

By default, only requests using the default style for a given layer will be cached. When this setting is enabled, all requests for a given layer, even those that use a non-standard style will be cached. Disabling this may be useful in situations where disk space is an issue, or when only one default style is important.

Default metatile size
~~~~~~~~~~~~~~~~~~~~~

A metatile is several tiles combined into a larger one. This larger metatile is generated and then subdivided before being served back (and cached) as standard tiles. The advantage of using metatiling is in situations where a label or geometry lies on a boundary of a tile, which may be truncated or altered. With metatiling, these tile edge issues are greatly reduced.

Moreover, with metatiling, the overall time it takes to seed the cache is reduced in most cases, when compared with rendering a full map with single tiles. In fact, using larger metatiling factors is a good way to reduce the time spent in seeding the cache. 

The disadvantage of metatiling is that at large sizes, memory consumption can be an issue.

The size of the default metatile can be adjusted here. By default, GeoServer sets a metatile size of **4x4**, which strikes a balance between performance, memory usage, and rendering accuracy.

Default gutter size
~~~~~~~~~~~~~~~~~~~

The gutter size sets the amount of extra space (in pixels) used when generating a tile. Use this in conjunction with metatiles to reduce problems with labels and features not being rendered incorrectly due to being on a tile boundary.

Default Cache Formats
~~~~~~~~~~~~~~~~~~~~~

This setting determines the default image formats that can be cached when tiled requests are made. There are four image formats that can be used when saving tiles:

* PNG (24-bit PNG)
* PNG8 (8-bit PNG)
* JPEG
* GIF

The default settings are subdivided into vector layers, raster layers, and layer groups. You may select any of the above four formats for each of the three types of layers. Any requests that fall outside of these layer/format combinations will not be cached if sent through GeoServer, and will return an error if sent to the GeoWebCache endpoints.

These defaults can be overwritten on a per-layer basis when :ref:`editing the layer properties <webadmin_layers>`.

.. figure:: img/defaults_formats.png
   :align: center

   *Default image formats*



Default Cached Gridsets
~~~~~~~~~~~~~~~~~~~~~~~

This section shows the gridsets that will be automatically configured for cached layers. While there are some pre-configured gridsets available, only two are enabled by default. These correspond to the most common and universal cases:

* EPSG:4326 (geographic) with 22 maximum zoom levels and 256x256 pixel tiles
* EPSG:900913 (spherical Mercator) with 31 maximum zoom levels and 256x256 pixel tiles

.. figure:: img/defaults_gridsets.png
   :align: center

   *Default gridsets*


To add a pre-existing grid set, select it from the :guilabel:`Add default grid set` menu, and click the Add icon (green circle with plus sign).

.. figure:: img/addexistinggridset.png
   :align: center

   *Adding an existing gridset to the list of defaults*

These definitions are described in more detail on the :ref:`webadmin_tilecaching_gridsets` page.
