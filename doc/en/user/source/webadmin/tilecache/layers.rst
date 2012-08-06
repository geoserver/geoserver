.. _webadmin_tilecaching_layers:

Tile Layers
===========

This page shows a listing of all of the layers known to the integrated GeoWebCache.  It can be thought of as a :ref:`layerpreview` page for GeoWebCache, and has many of the same options.

.. figure:: img/tilelayers.png
   :align: center

.. note:: There is also a link to the `GeoWebCache standalone demo page <webadmin_tilecaching_demopage>`.

Layer information
-----------------

For each layer being cached by GeoWebCache, the following information is available:


Disk Quota
~~~~~~~~~~

The maximum amount of disk space that can be used for this layer.  Will be set to :guilabel:`N/A` (unbounded) by default, unless :ref:`webadmin_tilecaching_diskquotas` are enabled.

Disk Used
~~~~~~~~~

The current disk space being used by tiles for this particular layer.

Enabled
~~~~~~~

Shows whether tile caching is enabled for this layer.  It is possible to have a layer definition here but to not have tile caching enabled; this is set in the layer properties.

Preview
~~~~~~~

Similar to the standard :ref:`layerpreview`, this will generate a simple OpenLayers application populated with tiles from one of the available gridset/image format combinations.  Select the desired option from the menu to view in OpenLayers.

Seed/Truncate
~~~~~~~~~~~~~

Brings up the GeoWebCache page for automatically seeding and truncating the tile cache.  Use this if you want to pre-populate some of your cache.

Empty
~~~~~

Will remove all saved tiles from the cache.  Operation is identical to a full truncate operation for the layer.



Add or remove cached layers
---------------------------

The list of layers displayed on this page is typically the same or similar as the full list of layers known to GeoServer.  However, it may not be desirable to have every layer published in GeoServer also have a cached layer component.  In this case, simply check the box next to the layer to remove, and click on :guilabel:`Remove selected cached layers`.  The layer will be removed from GeoWebCache, and the disk cache for this layer will be entirely removed.

.. warning:: The deletion of the tile cache is not undoable.

.. figure:: img/removecachedlayers.png
   :align: center

   *Removing a cached layer*

To add in a layer from GeoServer (if it wasn't set up to be added automatically), click on the :guilabel:`Add a new cached layer` link.  

.. figure:: img/newcachedlayer.png
   :align: center

   *Adding a new cached layer*

From here you have two options for layer configuration.  The first option is to load the layer using the default (global) settings.  To do this, select the layer you wish to start caching, and click the :guilabel:`Configure selected layers with caching defaults` link.  The second option is to configure the caching parameters manually, via the :ref:`layer configuration <webadmin_layers>` pages.  To do this, just click on the layer name itself.

