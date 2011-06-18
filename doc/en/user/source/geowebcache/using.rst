.. _gwc_using:

Using GeoWebCache
=================

.. note:: For an more in-depth discussion of using GeoWebCache, please see the `GeoWebCache documentation <http://geowebcache.org/docs/>`_.

GeoWebCache integration with GeoServer WMS
------------------------------------------

GeoWebCache (as of GeoServer 2.1.0) is transparently integrated with the GeoServer WMS, and so requires no special endpoint or custom URL in order to be used.  In this way one can have the simplicity of a standard WMS endpoint with the performance of a tiled client.

This direct integration is turned off by default.  It can be enabled by going to the :ref:`webadmin_gwc` page in the :ref:`web_admin`.

When this feature is enabled, GeoServer WMS will cache and retrieve tiles from GeoWebCache (via a GetMap request) **only if the following conditions apply**:

#. ``TILED=true`` is included in the request.
#. All other request parameters (tile height and width) match up with a tile in the layer's gridset.
#. There are no vendor-specific parameters (such as ``cql_filter``).

In addition, when direct integration is enabled, the WMS capabilities document (via a GetCapabilities request) will only return the WMS-C vendor-specific capabilities elements (such as a ``<TileSet>`` element for each cached layer/CRS/format combination) if ``TILED=true`` is appended to the GetCapabilities request.

.. note:: For more information on WMS-C, please see the `WMS Tiling Client Recommendation <http://wiki.osgeo.org/wiki/WMS_Tiling_Client_Recommendation>`_ from OSGeo.

.. note:: GeoWebCache integration is not compatible with the OpenLayers-based :ref:`layerpreview`, as the preview does not usually align with the GeoWebCache layer gridset.  This is because the OpenLayers application calculates the tileorigin based on the layer's bounding box, which is different from the gridset.  It is, however, very possible to create an OpenLayers application that caches tiles; just make sure that the tileorigin aligns with the gridset.

GeoWebCache endpoint URL
------------------------

When not using direct integration, you can point your client directly to GeoWebCache.

.. warning:: GeoWebCache is not a true WMS, and so the following is an oversimplification.  If you encounter errors, see the :ref:`gwc_troubleshooting` page for help. 

To direct your client to GeoWebCache (and thus receive cached tiles) you need to change the WMS URL.

If your application requests WMS tiles from GeoServer at this URL::

   http://example.com/geoserver/wms

You can invoke the GeoWebCache WMS instead at this URL::

   http://example.com/geoserver/gwc/service/wms
   
In other words, add ``/gwc/service/wms`` in between the path to your GeoServer instance and the WMS call.

As soon as tiles are requested through GeoWebCache, GeoWebCache automatically starts saving them.  This means that initial requests for tiles will not be accelerated since GeoServer will still need to generate the tiles.  To automate this process of requesting tiles, you can **seed** the cache.  See the section on :ref:`gwc_seeding` for more details.

.. _gwc_diskquota:

Disk quota
----------

GeoWebCache has a built-in disk quota feature to prevent disk space from growing unbounded.  Disk quotas are turned off by default, but can be configured on the :ref:`webadmin_gwc` page in the :ref:`web_admin`.  You can set the maximum size of the cache directory, poll interval, and what policy of tile removal to use when the quota is exceeded.  Tiles can be removed based on usage ("Least Frequently Used" or LFU) or timestamp ("Least Recently Used" or LRU).

Integration with external mapping sites
---------------------------------------

The documentation on the `GeoWebCache homepage <http://geowebcache.org>`_ contains examples for creating applications that integrate with Google Maps, Google Earth, Bing Maps, and more. 

Support for custom projections
------------------------------

The version of GeoWebCache that comes embedded in GeoServer automatically configures every layer served in GeoServer with the two most common projections:

* **EPSG:4326** (latitude/longitude)
* **EPSG:900913** (Spherical Mercator, the projection used in Google Maps)

If you need another projection, you can create a custom configuration file, :file:`geowebcache.xml`, in the same directory that contains the cache (see the :ref:`gwc_config` page for information on how to set this).  This configuration file is the same as used by the standalone version of GeoWebCache (see that documentation for more details).  The configuration syntax directly supports the most common WMS parameters such as style, palette, and background color.  To prevent conflicts, the layers in this file should be named differently from the ones that are loaded from GeoServer.

