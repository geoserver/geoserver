.. _webadmin_tilecaching_layers:

Tile Layers
===========

This page shows a listing of all of the layers known to the integrated GeoWebCache. It is similar to the :ref:`layerpreview` for GeoWebCache, with many of the same options.


.. figure:: img/tilelayers.png
   :align: center

.. note:: There is also a link to the :ref:`GeoWebCache standalone demo page <webadmin_tilecaching_demo>`.

Layer information
-----------------

For each layer cached by GeoWebCache, the following information is available.

Disk Quota
~~~~~~~~~~

The maximum amount of disk space that can be used for this layer. By default, this will be set to :guilabel:`N/A` (unbounded) unless :ref:`webadmin_tilecaching_diskquotas` are enabled.

Disk Used
~~~~~~~~~

The current disk space being used by tiles for this particular layer.

.. note:: **This counter will only be updated if disk quotas are enabled.** If disk quotas are not enabled, tiles will still be saved to disk, but the counter will remain as ``0.0 B``.

Enabled
~~~~~~~

Indicates whether tile caching is enabled for this layer. It is possible to have a layer definition here but to not have tile caching enabled (set in the layer properties).

Preview
~~~~~~~

Similar to :ref:`layerpreview`, this will generate a simple OpenLayers application populated with tiles from one of the available gridset/image format combinations. Select the desired option from the menu to view in OpenLayers.

Seed/Truncate
~~~~~~~~~~~~~

Opens the GeoWebCache page for automatically seeding and truncating the tile cache. Use this if you want to pre-populate some of your cache.

Empty
~~~~~

Will remove all saved tiles from the cache. This is identical to a full truncate operation for the layer.


Add or remove cached layers
---------------------------

The list of layers displayed on this page is typically the same as, or similar to, the full list of layers known to GeoServer. However, it may not be desirable to have every layer published in GeoServer have a cached layer component. In this case, simply select the box next to the layer to remove, and click :guilabel:`Remove selected cached layers`. The layer will be removed from GeoWebCache, and the disk cache for this layer will be entirely removed.

.. warning:: Deleting the tile cache cannot be undone.

.. figure:: img/removecachedlayers.png
   :align: center

   *Removing a cached layer*

To add in a layer from GeoServer (if it wasn't set up to be added automatically), click the :guilabel:`Add a new cached layer` link. 

.. figure:: img/newcachedlayer.png
   :align: center

   *Adding a new cached layer*

You have two options for layer configuration. The first option is to load the layer using the default (global) settings. To do this, select the layer you wish to start caching, and click the :guilabel:`Configure selected layers with caching defaults` link. The second option is to configure the caching parameters manually, via the :ref:`layer configuration <webadmin_layers>` pages. To do this, just click the layer name itself.

